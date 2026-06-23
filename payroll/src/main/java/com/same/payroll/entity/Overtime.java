package com.same.payroll.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "overtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Overtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal hours;

    // 1.25 = 25% extra, 1.50 = 50% extra, 2.0 = double time
    @Column(name = "rate_multiplier", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal rateMultiplier = new BigDecimal("1.25");

    @Column(name = "overtime_pay", precision = 10, scale = 3)
    private BigDecimal overtimePay;

    private Integer year;
    private Integer month;

    @Column(length = 300)
    private String reason;
}