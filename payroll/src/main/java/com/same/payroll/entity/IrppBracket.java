package com.same.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "irpp_brackets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrppBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_income", nullable = false, precision = 12, scale = 3)
    private BigDecimal minIncome;

    // null means no upper limit (last bracket)
    @Column(name = "max_income", precision = 12, scale = 3)
    private BigDecimal maxIncome;

    // e.g. 0.26 = 26%
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "bracket_order", nullable = false)
    private Integer bracketOrder;
}