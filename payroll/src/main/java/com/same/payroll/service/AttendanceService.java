package com.same.payroll.service;

import com.same.payroll.dto.AttendanceRecordDto;
import com.same.payroll.dto.MonthSummaryDto;
import com.same.payroll.dto.UpsertAttendanceRequest;
import com.same.payroll.entity.AttendanceRecord;
import com.same.payroll.entity.Employee;
import com.same.payroll.repository.AttendanceRecordRepository;
import com.same.payroll.repository.EmployeeRepository;
import com.same.payroll.repository.UserRepository;
import com.same.payroll.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business logic for the Pointage (attendance) module.
 *
 * Auth integration notes:
 *  - Write operations (upsert / delete) are already gated by @RequirePermission
 *    on the controller before they reach here, so the service does NOT need to
 *    repeat an access check — that would duplicate the aspect's job.
 *  - Read scoping (EMPLOYEE_VIEWER sees only their own row) is done here
 *    via PermissionService.hasRole() / getCurrentPrincipal(), which reads
 *    the CustomUserPrincipal straight from SecurityContextHolder — exactly
 *    the same source that the Redis session deserializes into.
 *  - We never accept a principal as a method parameter; the SecurityContext
 *    is always authoritative and always available on the same thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepo;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;  // your existing service

    // ─── Month Overview ───────────────────────────────────────────────────────

    public List<MonthSummaryDto> getYearSummary(Integer year) {
        LocalDate today = LocalDate.now();
        long totalActive = employeeRepository.countByStatus(Employee.EmployeeStatus.ACTIVE);

        List<MonthSummaryDto> summaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            long filled = attendanceRepo.countByYearAndMonth(year, month);
            String status;
            if (filled == 0)               status = "VIDE";
            else if (filled < totalActive) status = "EN_COURS";
            else                           status = "COMPLET";

            summaries.add(MonthSummaryDto.builder()
                    .year(year)
                    .month(month)
                    .totalEmployees((int) totalActive)
                    .filledCount((int) filled)
                    .status(status)
                    .currentMonth(today.getYear() == year && today.getMonthValue() == month)
                    .build());
        }
        return summaries;
    }

    // ─── Detail: list records for a month ────────────────────────────────────

    /**
     * Returns one AttendanceRecordDto per active employee.
     * If no record exists yet a blank dto is returned so the frontend always
     * has a row to render.
     *
     * Scoping rules (read from SecurityContextHolder via PermissionService):
     *   ADMIN / HR_MANAGER → all active employees, filters applied
     *   EMPLOYEE_VIEWER    → only their own linked employee row, no filters
     */
    public List<AttendanceRecordDto> getMonthDetail(Integer year, Integer month,
                                                    String nameFilter, String emailFilter,
                                                    String deptFilter) {
        // Use PermissionService.hasRole() — consistent with how every other
        // service in the app checks roles, and decoupled from HTTP layer.
        boolean isAdmin  = permissionService.hasRole("ADMIN");
        boolean isHr     = permissionService.hasRole("HR_MANAGER");
        boolean isViewer = !isAdmin && !isHr && permissionService.hasRole("EMPLOYEE_VIEWER");

        List<Employee> employees;

        if (isViewer) {
            // CustomUserPrincipal.getUserId() is the users.id PK.
            // User.employee is the linked Employee (set at first OAuth2 login).
            CustomUserPrincipal principal = permissionService.getCurrentPrincipal();
            employees = userRepository.findById(principal.getUserId())
                    .map(u -> u.getEmployee() != null
                            ? List.of(u.getEmployee())
                            : List.<Employee>of())
                    .orElse(List.of());
        } else {
            // HR_MANAGER / ADMIN see all active employees
            employees = employeeRepository.findAll().stream()
                    .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                    .collect(Collectors.toList());

            // Apply filters only for privileged roles
            if (nameFilter != null && !nameFilter.isBlank()) {
                String lc = nameFilter.toLowerCase();
                employees = employees.stream()
                        .filter(e -> e.getFullName() != null
                                && e.getFullName().toLowerCase().contains(lc))
                        .collect(Collectors.toList());
            }
            if (emailFilter != null && !emailFilter.isBlank()) {
                String lc = emailFilter.toLowerCase();
                employees = employees.stream()
                        .filter(e -> e.getEmail() != null
                                && e.getEmail().toLowerCase().contains(lc))
                        .collect(Collectors.toList());
            }
            if (deptFilter != null && !deptFilter.isBlank()) {
                String lc = deptFilter.toLowerCase();
                employees = employees.stream()
                        .filter(e -> e.getDepartment() != null
                                && e.getDepartment().getName() != null
                                && e.getDepartment().getName().toLowerCase().contains(lc))
                        .collect(Collectors.toList());
            }
        }

        // Index existing records for O(1) lookup
        List<AttendanceRecord> existing = attendanceRepo.findByYearAndMonth(year, month);
        Map<Long, AttendanceRecord> byEmpId = existing.stream()
                .collect(Collectors.toMap(r -> r.getEmployee().getId(), Function.identity()));

        int workingDays = computeWorkingDays(year, month);

        return employees.stream()
                .map(emp -> toDto(emp, byEmpId.get(emp.getId()), year, month, workingDays))
                .collect(Collectors.toList());
    }

    // ─── Upsert ──────────────────────────────────────────────────────────────

    /**
     * Create or update one employee's record for a month.
     * Access is pre-checked by @RequirePermission(ATTENDANCE:WRITE) on the
     * controller — no second check needed here.
     */
    @Transactional
    public AttendanceRecordDto upsert(UpsertAttendanceRequest req) {
        Employee emp = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + req.getEmployeeId()));

        AttendanceRecord rec = attendanceRepo
                .findByEmployeeIdAndYearAndMonth(req.getEmployeeId(), req.getYear(), req.getMonth())
                .orElseGet(() -> AttendanceRecord.builder()
                        .employee(emp)
                        .year(req.getYear())
                        .month(req.getMonth())
                        .advanceDeduction(BigDecimal.ZERO)
                        .build());

        applyRequest(rec, req);
        rec = attendanceRepo.save(rec);

        log.info("Upserted attendance for employee {} period {}/{} by user {}",
                emp.getId(), req.getYear(), req.getMonth(),
                permissionService.getCurrentPrincipal().getEmail());

        return toDto(emp, rec, req.getYear(), req.getMonth(),
                computeWorkingDays(req.getYear(), req.getMonth()));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    /**
     * Delete one employee's record for a month.
     * Access is pre-checked by @RequirePermission(ATTENDANCE:DELETE) on the
     * controller — no second check needed here.
     */
    @Transactional
    public void delete(Long employeeId, Integer year, Integer month) {
        attendanceRepo.deleteByEmployeeIdAndYearAndMonth(employeeId, year, month);
        log.info("Deleted attendance for employee {} period {}/{} by user {}",
                employeeId, year, month,
                permissionService.getCurrentPrincipal().getEmail());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void applyRequest(AttendanceRecord rec, UpsertAttendanceRequest req) {
        if (req.getHoursWorked()      != null) rec.setHoursWorked(req.getHoursWorked());
        if (req.getDaysWorked()       != null) rec.setDaysWorked(req.getDaysWorked());
        if (req.getPublicHolidays()   != null) rec.setPublicHolidays(req.getPublicHolidays());
        if (req.getLeaveDaysBase()    != null) rec.setLeaveDaysBase(req.getLeaveDaysBase());
        if (req.getLeavePay()         != null) rec.setLeavePay(req.getLeavePay());
        if (req.getAdvanceDeduction() != null) rec.setAdvanceDeduction(req.getAdvanceDeduction());
        if (req.getRegime()           != null) rec.setRegime(req.getRegime());
        if (req.getAffectation()      != null) rec.setAffectation(req.getAffectation());
        if (req.getService()          != null) rec.setService(req.getService());
        if (req.getSection()          != null) rec.setSection(req.getSection());
    }

    private AttendanceRecordDto toDto(Employee emp, AttendanceRecord rec,
                                      Integer year, Integer month, int workingDays) {
        return AttendanceRecordDto.builder()
                .id(rec != null ? rec.getId() : null)
                .employeeId(emp.getId())
                .employeeFullName(emp.getFullName())
                .employeeCode(emp.getEmployeeId())
                .email(emp.getEmail())
                .department(emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                .year(year)
                .month(month)
                .nJours(workingDays)
                .hoursWorked(rec != null ? rec.getHoursWorked() : null)
                .daysWorked(rec != null ? rec.getDaysWorked() : null)
                .publicHolidays(rec != null ? rec.getPublicHolidays() : null)
                .leaveDaysBase(rec != null ? rec.getLeaveDaysBase() : null)
                .leavePay(rec != null ? rec.getLeavePay() : null)
                .advanceDeduction(rec != null ? rec.getAdvanceDeduction() : null)
                .regime(rec != null ? rec.getRegime() : null)
                .affectation(rec != null ? rec.getAffectation() : null)
                .service(rec != null ? rec.getService() : null)
                .section(rec != null ? rec.getSection() : null)
                .filled(rec != null && isFilled(rec))
                .build();
    }

    private boolean isFilled(AttendanceRecord rec) {
        return isNonZero(rec.getHoursWorked())
                || isNonZero(rec.getDaysWorked())
                || isNonZero(rec.getPublicHolidays())
                || isNonZero(rec.getLeaveDaysBase())
                || isNonZero(rec.getLeavePay())
                || isNonZero(rec.getAdvanceDeduction());
    }

    private boolean isNonZero(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) != 0;
    }

    private int computeWorkingDays(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        int count = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            int dow = LocalDate.of(year, month, d).getDayOfWeek().getValue(); // 1=Mon..7=Sun
            if (dow < 6) count++;
        }
        return count;
    }
}