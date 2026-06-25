package com.same.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Fields an admin can edit directly on a user account. Email is
 * intentionally NOT editable here - it's tied to the linked Employee
 * record and Google identity, so changing it would break the
 * employee-email matching that gates login in the first place.
 */
@Data
public class UpdateUserRequest {

    @NotBlank
    private String fullName;
}