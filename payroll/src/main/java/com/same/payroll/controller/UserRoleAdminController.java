package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.AdminUserDto;
import com.same.payroll.service.UserRoleAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserRoleAdminController {

    private final UserRoleAssignmentService userRoleAssignmentService;

    /**
     * Replaces the user's roles with a single role - this is what the
     * admin user-management table's role dropdown calls. Body:
     * {"role": "ADMIN"}
     */
    @PatchMapping("/{userId}/role")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public AdminUserDto setRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        String roleName = body.get("role");
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        return userRoleAssignmentService.setSingleRole(userId, roleName);
    }

    /** Full multi-role replace, kept for cases that need more than one role per user. */
    @PutMapping("/{userId}/roles")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public AdminUserDto setRoles(@PathVariable Long userId, @RequestBody Set<String> roleNames) {
        return userRoleAssignmentService.assignRoles(userId, roleNames);
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public AdminUserDto addRole(@PathVariable Long userId, @PathVariable String roleName) {
        return userRoleAssignmentService.addRole(userId, roleName);
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public AdminUserDto removeRole(@PathVariable Long userId, @PathVariable String roleName) {
        return userRoleAssignmentService.removeRole(userId, roleName);
    }
}