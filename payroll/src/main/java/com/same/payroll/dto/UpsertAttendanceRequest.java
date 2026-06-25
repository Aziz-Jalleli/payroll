package com.same.payroll.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload for creating or updating a single employee's attendance record
 * for a given year/month. All numeric fields are optional — the caller
 * sends only the columns they've filled in; null means "unchanged / zero".
 */
@Data
public class UpsertAttendanceRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Integer year;

    @NotNull
    private Integer month;

    private BigDecimal absences;
    private BigDecimal conges;
    private BigDecimal hSupp75;
    private BigDecimal hSupp100;
    private BigDecimal hSupp25;
    private BigDecimal hNuit1;
    private BigDecimal hNuit2;
    private BigDecimal avances;
    private BigDecimal jFeries;
    private BigDecimal jfTravaille;
    private BigDecimal hoursWorked;
    private BigDecimal daysWorked;
    private BigDecimal publicHolidays;
    private BigDecimal leaveDaysBase;
    private BigDecimal leavePay;
    private BigDecimal advanceDeduction;
    private String regime;
    private String affectation;
    private String service;
    private String section;
}