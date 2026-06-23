package com.same.payroll.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year", "month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "hours_worked", precision = 8, scale = 2)
    private BigDecimal hoursWorked;

    @Column(name = "days_worked", precision = 6, scale = 2)
    private BigDecimal daysWorked;

    @Column(name = "public_holidays", precision = 6, scale = 2)
    private BigDecimal publicHolidays;

    @Column(name = "leave_days_base", precision = 6, scale = 2)
    private BigDecimal leaveDaysBase;

    @Column(name = "leave_pay", precision = 10, scale = 3)
    private BigDecimal leavePay;

    @Column(name = "advance_deduction", precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    @Column(name = "regime")
    private String regime;

    @Column(name = "affectation")
    private String affectation;

    @Column(name = "service")
    private String service;

    @Column(name = "section")
    private String section;
}