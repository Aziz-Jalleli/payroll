package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.AdminUserDto;
import com.same.payroll.dto.UpdateUserRequest;
import com.same.payroll.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @RequirePermission(resource = "USER", action = "READ")
    public List<AdminUserDto> listUsers() {
        return userManagementService.listUsers();
    }

    @GetMapping("/{id}")
    @RequirePermission(resource = "USER", action = "READ")
    public AdminUserDto getUser(@PathVariable Long id) {
        return userManagementService.getUser(id);
    }

    @PutMapping("/{id}")
    @RequirePermission(resource = "USER", action = "UPDATE")
    public AdminUserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userManagementService.updateUser(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @RequirePermission(resource = "USER", action = "UPDATE")
    public AdminUserDto deactivateUser(@PathVariable Long id) {
        return userManagementService.setEnabled(id, false);
    }

    @PatchMapping("/{id}/activate")
    @RequirePermission(resource = "USER", action = "UPDATE")
    public AdminUserDto activateUser(@PathVariable Long id) {
        return userManagementService.setEnabled(id, true);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(resource = "USER", action = "DELETE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
    }
}