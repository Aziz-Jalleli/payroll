package com.same.payroll.controller;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.dto.CreateRoleRequest;
import com.same.payroll.dto.RoleDto;
import com.same.payroll.service.RoleManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleManagementService roleManagementService;

    @PostMapping
    @RequirePermission(resource = "ROLE", action = "CREATE")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleManagementService.createRole(request);
    }

    @GetMapping
    @RequirePermission(resource = "ROLE", action = "READ")
    public List<RoleDto> listRoles() {
        return roleManagementService.listRoles();
    }

    @GetMapping("/{id}")
    @RequirePermission(resource = "ROLE", action = "READ")
    public RoleDto getRole(@PathVariable Long id) {
        return roleManagementService.getRole(id);
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @RequirePermission(resource = "ROLE", action = "UPDATE")
    public RoleDto addPermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        return roleManagementService.addPermission(roleId, permissionId);
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @RequirePermission(resource = "ROLE", action = "UPDATE")
    public RoleDto removePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        return roleManagementService.removePermission(roleId, permissionId);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(resource = "ROLE", action = "DELETE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long id) {
        roleManagementService.deleteRole(id);
    }
}