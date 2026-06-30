package com.same.payroll.service;

import com.same.payroll.dto.PayrollCalculationDto;
import com.same.payroll.dto.PayslipDto;
import com.same.payroll.entity.Employee;
import com.same.payroll.entity.FamilySituation;
import com.same.payroll.entity.Payslip;
import com.same.payroll.pdf.PayslipPdfGenerator;
import com.same.payroll.repository.EmployeeRepository;
import com.same.payroll.repository.FamilySituationRepository;
import com.same.payroll.repository.PayslipRepository;
import com.same.payroll.repository.UserRepository;
import com.same.payroll.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Access model for payslips (separate from the generic @RequirePermission
 * aspect because the rule is row-scoped, not just action-scoped):
 * <p>
 * PAYROLL:READ_ALL  -> can see/list every employee's payslips
 * PAYROLL:GENERATE  -> can generate, regenerate, lock/unlock payslips
 * (for ANY employee - generation is always an
 * admin/HR/payroll-officer action, never self-serve)
 * Neither above     -> "viewer": can only view/download a payslip that
 * ALREADY EXISTS for their own linked Employee row.
 * They cannot trigger generation themselves, and
 * cannot see anyone else's payslip, generated or not.
 * <p>
 * This mirrors the same SecurityContextHolder-based pattern AttendanceService
 * already uses for its own row-scoping, kept here rather than in the
 * controller so the rule lives in one place regardless of entry point.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final FamilySituationRepository familySituationRepository;
    private final PayrollCalculationService payrollCalculationService;
    private final PayslipPdfGenerator payslipPdfGenerator;
    private final PermissionService permissionService;

    /**
     * Generates (or regenerates, if unlocked) a payslip for one employee/
     * period. Always requires PAYROLL:GENERATE - there is no self-serve
     * path here, by design (the requirement is that a viewer can only see
     * a payslip someone else already generated for them).
     */
    @Transactional
    public PayslipDto generate(Long employeeId, Integer year, Integer month) {
        requireGeneratePermission();

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        Payslip payslip = payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElse(null);

        if (payslip != null && Boolean.TRUE.equals(payslip.getIsLocked())) {
            throw new IllegalStateException(
                    "Payslip for employee " + employeeId + " (" + year + "/" + month
                            + ") is locked - unlock it first to regenerate");
        }

        PayrollCalculationDto calc = payrollCalculationService.calculate(employeeId, year, month);

        String fonction = employee.getPosition() != null ? employee.getPosition().getTitle() : null;
        boolean isChefFamille = familySituationRepository.findByEmployeeId(employeeId)
                .map(fs -> fs.getMaritalStatus() == FamilySituation.MaritalStatus.MARRIED)
                .orElse(false);
        // No CNSS number field exists on Employee yet - see PayrollController note.
        byte[] pdf = payslipPdfGenerator.generate(calc, fonction, null, isChefFamille);

        LocalDateTime now = LocalDateTime.now();
        String currentEmail = currentUserEmail();

        if (payslip == null) {
            payslip = Payslip.builder()
                    .employee(employee)
                    .year(year)
                    .month(month)
                    .isLocked(false)
                    .generatedAt(now)
                    .generatedBy(currentEmail)
                    .build();
        } else {
            payslip.setRegeneratedAt(now);
        }

        payslip.setGrossSalary(calc.getGrossSalary());
        payslip.setCnssEmployeeAmount(calc.getCnssEmployeeAmount());
        payslip.setIrppMonthly(calc.getIrppMonthly());
        payslip.setTotalDeductions(calc.getTotalDeductions());
        payslip.setNetSalary(calc.getNetSalary());
        payslip.setTotalEmployerCost(calc.getTotalEmployerCost());
        payslip.setPdfContent(pdf);

        payslip = payslipRepository.save(payslip);
        log.info("Generated payslip for employee {} period {}/{} by {}", employeeId, year, month, currentEmail);

        return toDto(payslip);
    }

    @Transactional
    public PayslipDto setLocked(Long employeeId, Integer year, Integer month, boolean locked) {
        requireGeneratePermission();

        Payslip payslip = payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payslip exists yet for employee " + employeeId + " (" + year + "/" + month + ")"));

        payslip.setIsLocked(locked);
        return toDto(payslipRepository.save(payslip));
    }

    /**
     * Lists payslip status for every employee in a period - requires
     * PAYROLL:READ_ALL. This is what feeds the main admin table's
     * Fiche/Etat columns.
     */
    @Transactional(readOnly = true)
    public List<PayslipDto> listForPeriod(Integer year, Integer month) {
        if (!permissionService.hasPermission("PAYROLL", "READ_ALL")) {
            throw new AccessDeniedException("Requires PAYROLL:READ_ALL to view all employees' payslips");
        }
        log.info("Im in");
        return payslipRepository.findByYearAndMonth(year, month).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<PayslipDto> getMyPayslip(Integer year, Integer month) {
        CustomUserPrincipal principal = permissionService.getCurrentPrincipal();
        if (principal == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Long ownEmployeeId = userRepository.findById(principal.getUserId())
                .map(u -> u.getEmployee() != null ? u.getEmployee().getId() : null)
                .orElse(null);

        if (ownEmployeeId == null) {
            return Optional.empty(); // user has no linked employee row
        }

        return payslipRepository.findByEmployeeIdAndYearAndMonth(ownEmployeeId, year, month)
                .map(this::toDto);
    }

    /**
     * Returns the PDF bytes for one employee/period, enforcing the
     * own-payslip-only rule for viewers. Returns empty if no payslip has
     * been generated yet (viewers never trigger generation themselves).
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> getPdf(Long employeeId, Integer year, Integer month) {
        assertCanView(employeeId);
        return payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .map(Payslip::getPdfContent);
    }
    @Transactional(readOnly = true)
    public Optional<PayslipDto> getStatus(Long employeeId, Integer year, Integer month) {
        assertCanView(employeeId);
        return payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .map(this::toDto);
    }

    // ─── Access control helpers ──────────────────────────────────

    private void requireGeneratePermission() {
        if (!permissionService.hasPermission("PAYROLL", "GENERATE")) {
            throw new AccessDeniedException("Requires PAYROLL:GENERATE to generate or lock payslips");
        }
    }

    /**
     * A user with PAYROLL:READ_ALL (or GENERATE, which implies admin-level
     * access in practice) may view any employee's payslip. Otherwise the
     * caller may only view their own - resolved via their linked Employee
     * row on the User entity, exactly like AttendanceService's viewer scoping.
     */
    private void assertCanView(Long employeeId) {
        boolean canViewAll = permissionService.hasPermission("PAYROLL", "READ_ALL")
                || permissionService.hasPermission("PAYROLL", "GENERATE");
        if (canViewAll) {
            return;
        }

        CustomUserPrincipal principal = permissionService.getCurrentPrincipal();
        if (principal == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Long ownEmployeeId = userRepository.findById(principal.getUserId())
                .map(u -> u.getEmployee() != null ? u.getEmployee().getId() : null)
                .orElse(null);

        if (ownEmployeeId == null || !ownEmployeeId.equals(employeeId)) {
            throw new AccessDeniedException("You can only view your own payslip");
        }
    }

    private String currentUserEmail() {
        CustomUserPrincipal principal = permissionService.getCurrentPrincipal();
        return principal != null ? principal.getEmail() : "unknown";
    }

    private PayslipDto toDto(Payslip payslip) {
        return PayslipDto.builder()
                .id(payslip.getId())
                .employeeId(payslip.getEmployee().getId())
                .employeeCode(payslip.getEmployee().getEmployeeId())
                .employeeFullName(payslip.getEmployee().getFullName())
                .year(payslip.getYear())
                .month(payslip.getMonth())
                .generated(true)
                .locked(Boolean.TRUE.equals(payslip.getIsLocked()))
                .generatedAt(payslip.getGeneratedAt())
                .generatedBy(payslip.getGeneratedBy())
                .grossSalary(payslip.getGrossSalary())
                .netSalary(payslip.getNetSalary())
                .totalDeductions(payslip.getTotalDeductions())
                .build();
    }
}