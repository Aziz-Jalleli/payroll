package com.same.payroll.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OvertimeDto {
    private Long id;
    private Long employeeId;
    private LocalDate date;
    private BigDecimal hours;
    private BigDecimal rateMultiplier;
    private BigDecimal overtimePay;
    private Integer year;
    private Integer month;
    private String reason;
}