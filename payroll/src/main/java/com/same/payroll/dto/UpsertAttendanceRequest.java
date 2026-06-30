package com.same.payroll.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload for creating or updating one employee's attendance record.
 *
 * Fields map 1-to-1 to AttendanceRecord entity columns — no invented fields.
 * All numeric/text fields are nullable: null means "don't touch this column"
 * (the service applies null-safe updates), so callers only need to send
 * the columns they actually changed.
 */
@Data
public class UpsertAttendanceRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Integer year;

    @NotNull
    private Integer month;

    /** hours_worked */
    private BigDecimal hoursWorked;

    /** days_worked */
    private BigDecimal daysWorked;

    /** public_holidays */
    private BigDecimal publicHolidays;

    /** leave_days_base */
    private BigDecimal leaveDaysBase;

    /** leave_pay */
    private BigDecimal leavePay;

    /** advance_deduction */
    private BigDecimal advanceDeduction;

    /** regime (text) */
    private String regime;

    /** affectation (text) */
    private String affectation;

    /** service (text) */
    private String service;

    /** section (text) */
    private String section;
}