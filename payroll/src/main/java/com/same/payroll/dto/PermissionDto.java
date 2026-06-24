package com.same.payroll.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionDto {
    private Long id;
    private String resource;
    private String action;
    private String description;
}