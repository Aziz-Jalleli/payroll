package com.same.payroll.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a generated payslip for one employee/period. Unlike
 * PayrollCalculationService.calculate(), which recomputes everything live
 * from current AttendanceRecord/SalaryStructure/etc data, a Payslip is a
 * point-in-time record: once generated, its amounts are frozen (snapshot
 * columns below) so editing attendance data afterward does NOT silently
 * change a payslip that's already been generated or locked - that would
 * be incorrect for payroll/legal purposes. "Verrouiller" (lock) prevents
 * regeneration entirely; an unlocked-but-generated payslip can still be
 * regenerated (overwriting the snapshot) if attendance data changes.
 */
@Entity
@Table(name = "payslips",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year", "month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

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

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /** Email of the admin/HR user who generated this payslip - for audit purposes. */
    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "regenerated_at")
    private LocalDateTime regeneratedAt;

    // ── Frozen snapshot of the calculation at generation time ──────────
    // Mirrors PayrollCalculationDto's numeric fields so a locked payslip's
    // displayed/downloaded values never drift from what was generated.
    @Column(name = "gross_salary", precision = 12, scale = 3)
    private BigDecimal grossSalary;

    @Column(name = "cnss_employee_amount", precision = 12, scale = 3)
    private BigDecimal cnssEmployeeAmount;

    @Column(name = "irpp_monthly", precision = 12, scale = 3)
    private BigDecimal irppMonthly;

    @Column(name = "total_deductions", precision = 12, scale = 3)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", precision = 12, scale = 3)
    private BigDecimal netSalary;

    @Column(name = "total_employer_cost", precision = 12, scale = 3)
    private BigDecimal totalEmployerCost;

    /** Full PDF, stored so "view/download" doesn't require recomputation later. */
    @Lob
    @Column(name = "pdf_content")
    private byte[] pdfContent;
}