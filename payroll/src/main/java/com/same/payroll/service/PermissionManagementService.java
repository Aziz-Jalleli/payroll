package com.same.payroll.service;

import com.same.payroll.dto.CreatePermissionRequest;
import com.same.payroll.dto.PermissionDto;
import com.same.payroll.entity.Permission;
import com.same.payroll.entity.Role;
import com.same.payroll.repository.PermissionRepository;
import com.same.payroll.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionManagementService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public PermissionDto createPermission(CreatePermissionRequest request) {
        String resource = request.getResource().trim().toUpperCase();
        String action = request.getAction().trim().toUpperCase();

        if (permissionRepository.existsByResourceAndAction(resource, action)) {
            throw new IllegalArgumentException(
                    "Permission already exists: " + resource + ":" + action
            );
        }

        Permission permission = Permission.builder()
                .resource(resource)
                .action(action)
                .description(request.getDescription())
                .build();
        permission = permissionRepository.save(permission);

        // Optional immediate attachment to one or more existing roles.
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            for (String roleName : request.getRoleNames()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                role.getPermissions().add(permission);
                roleRepository.save(role);
            }
        }

        return toDto(permission);
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PermissionDto getPermission(Long id) {
        return permissionRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
        try {
            permissionRepository.delete(permission);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Cannot delete permission " + id + " - it is still attached to one or more roles", ex
            );
        }
    }

    private PermissionDto toDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .build();
    }
}