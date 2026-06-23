package com.same.payroll.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "employee_bonuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeBonus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private Integer year;
    private Integer month;

    @Column(name = "bonus_gross", precision = 10, scale = 3)
    private BigDecimal bonusGross;

    @Column(name = "bonus_rappel", precision = 10, scale = 3)
    private BigDecimal bonusRappel;

    @Column(name = "gratification_note", length = 500)
    private String gratificationNote;
}