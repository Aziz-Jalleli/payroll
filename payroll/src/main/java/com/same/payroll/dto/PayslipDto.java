package com.same.payroll.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayslipDto {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeFullName;
    private Integer year;
    private Integer month;
    private boolean generated;
    private boolean locked;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal totalDeductions;
}