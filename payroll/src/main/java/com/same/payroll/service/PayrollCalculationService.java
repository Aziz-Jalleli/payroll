package com.same.payroll.service;

import com.same.payroll.dto.PayrollCalculationDto;
import com.same.payroll.entity.*;
import com.same.payroll.entity.SalaryStructure.SalaryBasis;
import com.same.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollCalculationService {

    private final EmployeeRepository          employeeRepository;
    private final SalaryStructureRepository   salaryStructureRepository;
    private final AttendanceRecordRepository  attendanceRecordRepository;
    private final AllowanceRepository         allowanceRepository;
    private final OvertimeRepository          overtimeRepository;
    private final EmployeeBonusRepository     employeeBonusRepository;
    private final FamilySituationRepository   familySituationRepository;
    private final CnssRateRepository          cnssRateRepository;
    private final IrppBracketRepository       irppBracketRepository;
    private final LegalConfigRepository       legalConfigRepository;

    private static final int SCALE = 3;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    // ─── Calculate for one employee ─────────────────────────────
    public PayrollCalculationDto calculate(Long employeeId, Integer year, Integer month) {

        // ── 1. Load core data ────────────────────────────────────
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        SalaryStructure salary = salaryStructureRepository
                .findByEmployeeIdAndIsCurrentTrue(employeeId)
                .orElseThrow(() -> new RuntimeException("No active salary for employee: " + employeeId));

        AttendanceRecord attendance = attendanceRecordRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new RuntimeException(
                        "No attendance record for employee " + employeeId + " - " + year + "/" + month));

        CnssRate cnssRate = cnssRateRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("No current CNSS rate configured"));

        List<IrppBracket> irppBrackets = irppBracketRepository
                .findByFiscalYearOrderByBracketOrderAsc(year);
        if (irppBrackets.isEmpty()) {
            // Fallback to latest available year
            irppBrackets = irppBracketRepository
                    .findAllByOrderByFiscalYearDescBracketOrderAsc()
                    .stream().limit(5).toList();
        }

        // ── 2. Working days base ─────────────────────────────────
        BigDecimal workingDaysBase;
        if (attendance.getRegime() != null && attendance.getRegime().matches("\\d+")) {
            workingDaysBase = new BigDecimal(attendance.getRegime());
        } else {
            workingDaysBase = new BigDecimal(salary.getWorkingDaysBase());
        }

        BigDecimal daysWorked = attendance.getDaysWorked() != null
                ? attendance.getDaysWorked() : workingDaysBase;

        BigDecimal absenceDays = workingDaysBase.subtract(daysWorked).max(BigDecimal.ZERO);

        // ── 3. Earned base salary (prorated) ────────────────────
        BigDecimal baseSalary = salary.getBaseSalary();
        BigDecimal earnedBaseSalary;

        if (salary.getSalaryBasis() == SalaryBasis.MONTHLY) {
            // Daily rate = base / working_days_base
            BigDecimal dailyRate = baseSalary.divide(workingDaysBase, SCALE, RM);
            earnedBaseSalary = dailyRate.multiply(daysWorked).setScale(SCALE, RM);
        } else if (salary.getSalaryBasis() == SalaryBasis.DAILY) {
            BigDecimal dailyRate = baseSalary;
            earnedBaseSalary = dailyRate.multiply(daysWorked).setScale(SCALE, RM);
        } else {
            // HOURLY
            BigDecimal hoursWorked = attendance.getHoursWorked() != null
                    ? attendance.getHoursWorked() : BigDecimal.ZERO;
            earnedBaseSalary = baseSalary.multiply(hoursWorked).setScale(SCALE, RM);
        }

        // ── 4. Allowances (recurring only) ───────────────────────
        List<Allowance> allowances = allowanceRepository
                .findByEmployeeIdAndIsRecurringTrue(employeeId);

        BigDecimal totalAllowances = allowances.stream()
                .map(Allowance::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RM);

        // ── 5. Overtime ──────────────────────────────────────────
        BigDecimal hourlyRate = baseSalary
                .divide(workingDaysBase, SCALE, RM)
                .divide(new BigDecimal("8"), SCALE, RM); // 8h per day

        BigDecimal weightedOvertimeHours = overtimeRepository
                .sumWeightedOvertimeHours(employeeId, year, month);

        BigDecimal overtimePay = hourlyRate
                .multiply(weightedOvertimeHours)
                .setScale(SCALE, RM);

        // ── 6. Bonuses ───────────────────────────────────────────
        List<EmployeeBonus> bonuses = employeeBonusRepository
                .findByYearAndMonth(year, month)
                .stream()
                .filter(b -> b.getEmployee().getId().equals(employeeId))
                .toList();

        BigDecimal bonusGross = bonuses.stream()
                .map(b -> b.getBonusGross() != null ? b.getBonusGross() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bonusRappel = bonuses.stream()
                .map(b -> b.getBonusRappel() != null ? b.getBonusRappel() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── 7. Leave pay ─────────────────────────────────────────
        BigDecimal leavePay = attendance.getLeavePay() != null
                ? attendance.getLeavePay() : BigDecimal.ZERO;

        // ── 8. Gross salary ──────────────────────────────────────
        BigDecimal grossSalary = earnedBaseSalary
                .add(totalAllowances)
                .add(overtimePay)
                .add(bonusGross)
                .add(bonusRappel)
                .add(leavePay)
                .setScale(SCALE, RM);

        // ── 9. CNSS ──────────────────────────────────────────────
        // No ceiling in Tunisia — applied on full gross
        BigDecimal cnssBase = grossSalary;
        BigDecimal cnssEmployee = cnssBase
                .multiply(cnssRate.getEmployeeRate())
                .setScale(SCALE, RM);
        BigDecimal cnssEmployer = cnssBase
                .multiply(cnssRate.getEmployerRate())
                .setScale(SCALE, RM);

        // ── 10. Taxable income (net CNSS) ────────────────────────
        BigDecimal taxableMonthly = grossSalary.subtract(cnssEmployee).setScale(SCALE, RM);
        BigDecimal taxableAnnual  = taxableMonthly.multiply(new BigDecimal("12")).setScale(SCALE, RM);

        // ── 11. Family deductions (IRPP) ─────────────────────────
        BigDecimal familyDeduction = BigDecimal.ZERO;
        Optional<FamilySituation> familyOpt = familySituationRepository
                .findByEmployeeId(employeeId);

        if (familyOpt.isPresent()) {
            FamilySituation family = familyOpt.get();

            BigDecimal chefFamilleDeduction = getConfig("DEDUCTION_CHEF_FAMILLE", "300.000");
            BigDecimal perChildDeduction     = getConfig("DEDUCTION_PER_CHILD",    "100.000");
            int maxChildren = getConfigInt("MAX_CHILDREN_DEDUCTION", 4);

            if (family.getMaritalStatus() == FamilySituation.MaritalStatus.MARRIED) {
                familyDeduction = familyDeduction.add(chefFamilleDeduction);
            }

            int eligibleChildren = Math.min(family.getNumberOfChildren(), maxChildren);
            familyDeduction = familyDeduction
                    .add(perChildDeduction.multiply(new BigDecimal(eligibleChildren)));
        }

        // ── 12. IRPP calculation (annual brackets) ───────────────
        BigDecimal netAnnualForIrpp = taxableAnnual.subtract(familyDeduction).max(BigDecimal.ZERO);
        BigDecimal irppAnnual = calculateIrpp(netAnnualForIrpp, irppBrackets);
        BigDecimal irppMonthly = irppAnnual.divide(new BigDecimal("12"), SCALE, RM);

        // ── 13. Advance deduction ────────────────────────────────
        BigDecimal advanceDeduction = attendance.getAdvanceDeduction() != null
                ? attendance.getAdvanceDeduction() : BigDecimal.ZERO;

        // ── 14. Net salary ───────────────────────────────────────
        BigDecimal totalDeductions = cnssEmployee
                .add(irppMonthly)
                .add(advanceDeduction)
                .setScale(SCALE, RM);

        BigDecimal netSalary = grossSalary.subtract(totalDeductions).setScale(SCALE, RM);

        // ── 15. Employer total cost ───────────────────────────────
        BigDecimal totalEmployerCost = grossSalary.add(cnssEmployer).setScale(SCALE, RM);

        // ── Build result ─────────────────────────────────────────
        return PayrollCalculationDto.builder()
                .employeeId(employeeId)
                .employeeCode(employee.getEmployeeId())
                .fullName(employee.getFullName())
                .year(year)
                .month(month)
                .workingDaysBase(workingDaysBase)
                .daysWorked(daysWorked)
                .hoursWorked(attendance.getHoursWorked())
                .absenceDays(absenceDays)
                .publicHolidays(attendance.getPublicHolidays())
                .baseSalary(baseSalary)
                .earnedBaseSalary(earnedBaseSalary)
                .totalAllowances(totalAllowances)
                .overtimePay(overtimePay)
                .bonusGross(bonusGross)
                .bonusRappel(bonusRappel)
                .leavePay(leavePay)
                .grossSalary(grossSalary)
                .cnssBase(cnssBase)
                .cnssEmployeeAmount(cnssEmployee)
                .cnssEmployerAmount(cnssEmployer)
                .taxableIncome(taxableMonthly)
                .annualTaxableIncome(taxableAnnual)
                .familyDeduction(familyDeduction)
                .irppAnnual(irppAnnual)
                .irppMonthly(irppMonthly)
                .advanceDeduction(advanceDeduction)
                .totalDeductions(totalDeductions)
                .netSalary(netSalary)
                .totalEmployerCost(totalEmployerCost)
                .build();
    }

    // ─── Calculate for all employees in a month ──────────────────
    public List<PayrollCalculationDto> calculateAll(Integer year, Integer month) {
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByYearAndMonth(year, month);

        return records.stream()
                .map(r -> {
                    try {
                        return calculate(r.getEmployee().getId(), year, month);
                    } catch (Exception e) {
                        log.error("Failed to calculate for employee {}: {}",
                                r.getEmployee().getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();
    }

    // ─── IRPP progressive bracket calculation ────────────────────
    private BigDecimal calculateIrpp(BigDecimal annualIncome, List<IrppBracket> brackets) {
        BigDecimal tax = BigDecimal.ZERO;

        for (IrppBracket bracket : brackets) {
            if (annualIncome.compareTo(bracket.getMinIncome()) <= 0) break;

            BigDecimal upper = bracket.getMaxIncome() != null
                    ? bracket.getMaxIncome()
                    : annualIncome; // last bracket: no ceiling

            BigDecimal taxableInBracket = annualIncome.min(upper)
                    .subtract(bracket.getMinIncome())
                    .max(BigDecimal.ZERO);

            tax = tax.add(taxableInBracket.multiply(bracket.getRate()));
        }

        return tax.setScale(SCALE, RM);
    }

    // ─── Config helpers ──────────────────────────────────────────
    private BigDecimal getConfig(String key, String defaultValue) {
        return legalConfigRepository.findByKey(key)
                .map(c -> new BigDecimal(c.getValue()))
                .orElse(new BigDecimal(defaultValue));
    }

    private int getConfigInt(String key, int defaultValue) {
        return legalConfigRepository.findByKey(key)
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(defaultValue);
    }
}