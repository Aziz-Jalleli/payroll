package com.same.payroll.service;

import com.same.payroll.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    /**
     * Checks whether the currently authenticated user holds a permission
     * matching (resource, action). Resource/action comparison is
     * case-insensitive.
     */
    public boolean hasPermission(String resource, String action) {
        CustomUserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            return false;
        }
        return principal.hasPermission(resource, action);
    }

    public boolean hasRole(String roleName) {
        CustomUserPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            return false;
        }
        return principal.getRoleNames().stream()
                .anyMatch(r -> r.equalsIgnoreCase(roleName));
    }

    public CustomUserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            return customUserPrincipal;
        }
        return null;
    }

    public Long getCurrentUserId() {
        CustomUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.getUserId() : null;
    }
}