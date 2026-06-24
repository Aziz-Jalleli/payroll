package com.same.payroll.controller;

import com.same.payroll.dto.CurrentUserDto;
import com.same.payroll.security.CustomUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
public class CurrentUserController {
    @GetMapping("/api/me")
    public ResponseEntity<CurrentUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object rawPrincipal = authentication.getPrincipal();

        if (!(rawPrincipal instanceof CustomUserPrincipal principal)) {
            // Log what type it actually is so you can diagnose further
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(CurrentUserDto.builder()
                .id(principal.getUserId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .roles(principal.getRoleNames())
                .permissions(principal.getPermissionAuthorities())
                .build());
    }
}