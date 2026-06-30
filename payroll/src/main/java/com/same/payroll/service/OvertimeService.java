package com.same.payroll.service;

import com.same.payroll.dto.CreateOvertimeRequest;
import com.same.payroll.dto.OvertimeDto;
import com.same.payroll.entity.Employee;
import com.same.payroll.entity.Overtime;
import com.same.payroll.repository.EmployeeRepository;
import com.same.payroll.repository.OvertimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Backs the overtime entry modal (add/remove individual dated overtime
 * records) and the read-only weighted-sum H.SUPP column on the main
 * payroll table. Each Overtime row is one date-stamped occurrence with
 * its own hours and rate multiplier - there's no single "overtime hours"
 * field to inline-edit on AttendanceRecord, which is why this is a
 * separate small CRUD surface rather than a table cell.
 */
@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeRepository overtimeRepository;
    private final EmployeeRepository employeeRepository;

    public List<OvertimeDto> listForEmployeePeriod(Long employeeId, Integer year, Integer month) {
        return overtimeRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public BigDecimal getWeightedSum(Long employeeId, Integer year, Integer month) {
        return overtimeRepository.sumWeightedOvertimeHours(employeeId, year, month);
    }

    @Transactional
    public OvertimeDto create(CreateOvertimeRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + request.getEmployeeId()));

        BigDecimal rateMultiplier = request.getRateMultiplier() != null
                ? request.getRateMultiplier() : new BigDecimal("1.25");

        BigDecimal overtimePay = null; // computed at payroll-calculation time, not stored eagerly here

        Overtime entry = Overtime.builder()
                .employee(employee)
                .date(request.getDate())
                .hours(request.getHours())
                .rateMultiplier(rateMultiplier)
                .overtimePay(overtimePay)
                .year(request.getDate().getYear())
                .month(request.getDate().getMonthValue())
                .reason(request.getReason())
                .build();

        return toDto(overtimeRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        if (!overtimeRepository.existsById(id)) {
            throw new IllegalArgumentException("Overtime entry not found: " + id);
        }
        overtimeRepository.deleteById(id);
    }

    private OvertimeDto toDto(Overtime entry) {
        return OvertimeDto.builder()
                .id(entry.getId())
                .employeeId(entry.getEmployee().getId())
                .date(entry.getDate())
                .hours(entry.getHours())
                .rateMultiplier(entry.getRateMultiplier())
                .overtimePay(entry.getOvertimePay())
                .year(entry.getYear())
                .month(entry.getMonth())
                .reason(entry.getReason())
                .build();
    }
}