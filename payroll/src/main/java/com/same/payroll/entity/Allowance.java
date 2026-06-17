package com.same.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "allowances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allowance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllowanceType type;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal amount;

    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = true;

    @Column(name = "is_taxable")
    @Builder.Default
    private Boolean isTaxable = true;

    public enum AllowanceType {
        TRANSPORT,
        HOUSING,
        MEAL,
        PHONE,
        REPRESENTATION,
        OTHER
    }
}