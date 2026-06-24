package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.entity.User;
import com.same.payroll.service.UserRoleAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserRoleAdminController {

    private final UserRoleAssignmentService userRoleAssignmentService;

    @PutMapping("/{userId}/roles")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public User setRoles(@PathVariable Long userId, @RequestBody Set<String> roleNames) {
        return userRoleAssignmentService.assignRoles(userId, roleNames);
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public User addRole(@PathVariable Long userId, @PathVariable String roleName) {
        return userRoleAssignmentService.addRole(userId, roleName);
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @RequirePermission(resource = "USER_ROLE", action = "UPDATE")
    public User removeRole(@PathVariable Long userId, @PathVariable String roleName) {
        return userRoleAssignmentService.removeRole(userId, roleName);
    }
}