package com.same.payroll.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AttendanceRecordDto {
    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private String employeeCode;   // e.g. EMP001
    private String email;
    private String department;
    private Integer year;
    private Integer month;
    private Integer nJours;        // working days in the month (computed, read-only)
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
    private boolean filled;        // true if any numeric field has been entered
}