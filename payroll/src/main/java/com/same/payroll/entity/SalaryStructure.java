package com.same.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_structures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "base_salary", nullable = false, precision = 10, scale = 3)
    private BigDecimal baseSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_basis", nullable = false)
    @Builder.Default
    private SalaryBasis salaryBasis = SalaryBasis.MONTHLY;

    // Base working days used to compute daily rate: 22, 24, 26, etc.
    @Column(name = "working_days_base", nullable = false)
    @Builder.Default
    private Integer workingDaysBase = 22;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = true;

    public enum SalaryBasis {
        MONTHLY,   // fixed monthly salary
        DAILY,     // paid per day worked
        HOURLY     // paid per hour worked
    }
}