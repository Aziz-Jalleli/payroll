package com.same.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class CreateRoleRequest {

    @NotBlank
    private String name;

    private String description;

    /**
     * Optional. IDs of existing permissions to attach to this role
     * immediately. If null/empty, the role is created with no permissions
     * and they can be attached later.
     */
    private Set<Long> permissionIds;
}