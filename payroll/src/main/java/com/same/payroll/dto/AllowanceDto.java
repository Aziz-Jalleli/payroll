package com.same.payroll.dto;

import com.same.payroll.entity.Allowance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceDto {
    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private String employeeCode;
    private Allowance.AllowanceType type;
    private BigDecimal amount;
    private Boolean isRecurring;
    private Boolean isTaxable;
}