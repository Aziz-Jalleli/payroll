package com.same.payroll.dto;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCalculationDto {

    // Identity
    private Long employeeId;
    private String employeeCode;
    private String fullName;
    private Integer year;
    private Integer month;

    // Attendance
    private BigDecimal workingDaysBase;   // base days in month (e.g. 22)
    private BigDecimal daysWorked;
    private BigDecimal hoursWorked;
    private BigDecimal absenceDays;
    private BigDecimal publicHolidays;

    // Earnings
    private BigDecimal baseSalary;        // full monthly base
    private BigDecimal earnedBaseSalary;  // prorated if absent
    private BigDecimal totalAllowances;
    private BigDecimal overtimePay;
    private BigDecimal bonusGross;
    private BigDecimal bonusRappel;
    private BigDecimal leavePay;

    // Gross
    private BigDecimal grossSalary;       // earned base + allowances + overtime + bonuses + leave

    // CNSS
    private BigDecimal cnssBase;          // gross subject to CNSS
    private BigDecimal cnssEmployeeAmount; // 9.18% employee share
    private BigDecimal cnssEmployerAmount; // 16.57% employer share

    // IRPP
    private BigDecimal taxableIncome;     // gross - CNSS employee
    private BigDecimal annualTaxableIncome;
    private BigDecimal familyDeduction;   // chef de famille + children
    private BigDecimal irppAnnual;        // annual IRPP
    private BigDecimal irppMonthly;       // monthly withholding

    // Other deductions
    private BigDecimal advanceDeduction;

    // Net
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;

    // Employer cost
    private BigDecimal totalEmployerCost; // gross + CNSS employer
}
