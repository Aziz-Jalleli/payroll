package com.same.payroll.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateOvertimeRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate date;

    @NotNull
    private BigDecimal hours;

    private BigDecimal rateMultiplier;

    private String reason;
}