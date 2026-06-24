package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.CreatePermissionRequest;
import com.same.payroll.dto.PermissionDto;
import com.same.payroll.service.PermissionManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final PermissionManagementService permissionManagementService;

    @PostMapping
    @RequirePermission(resource = "PERMISSION", action = "CREATE")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionDto createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return permissionManagementService.createPermission(request);
    }

    @GetMapping
    @RequirePermission(resource = "PERMISSION", action = "READ")
    public List<PermissionDto> listPermissions() {
        return permissionManagementService.listPermissions();
    }

    @GetMapping("/{id}")
    @RequirePermission(resource = "PERMISSION", action = "READ")
    public PermissionDto getPermission(@PathVariable Long id) {
        return permissionManagementService.getPermission(id);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(resource = "PERMISSION", action = "DELETE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermission(@PathVariable Long id) {
        permissionManagementService.deletePermission(id);
    }
}