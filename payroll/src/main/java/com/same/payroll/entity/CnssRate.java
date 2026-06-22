package com.same.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cnss_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CnssRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal employeeRate; // e.g. 0.0918

    @Column(name = "employer_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal employerRate; // e.g. 0.1657

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = true;

    @Column(length = 500)
    private String description;
}