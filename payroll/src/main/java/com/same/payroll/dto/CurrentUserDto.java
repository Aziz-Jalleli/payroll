package com.same.payroll.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CurrentUserDto {
    private Long id;
    private String email;
    private String fullName;
    private String pictureUrl;
    private Set<String> roles;
    private Set<String> permissions;
}