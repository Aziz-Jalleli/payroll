package com.same.payroll.dto;

import com.same.payroll.entity.Allowance;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertAllowanceRequest {

    @NotNull(message = "L'employé est obligatoire")
    private Long employeeId;

    @NotNull(message = "Le type est obligatoire")
    private Allowance.AllowanceType type;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private BigDecimal amount;

    private Boolean isRecurring = true;
    private Boolean isTaxable   = true;
}