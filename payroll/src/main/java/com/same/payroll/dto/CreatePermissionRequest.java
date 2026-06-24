package com.same.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class CreatePermissionRequest {

    @NotBlank
    private String resource;

    @NotBlank
    private String action;

    private String description;

    /**
     * Optional. Names of existing roles to attach this permission to
     * immediately. If null/empty, the permission is created unattached
     * and can be assigned to roles later.
     */
    private Set<String> roleNames;
}