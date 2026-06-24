package com.same.payroll.service;

import com.same.payroll.dto.CreateRoleRequest;
import com.same.payroll.dto.PermissionDto;
import com.same.payroll.dto.RoleDto;
import com.same.payroll.entity.Permission;
import com.same.payroll.entity.Role;
import com.same.payroll.repository.PermissionRepository;
import com.same.payroll.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        String name = request.getName().trim().toUpperCase();

        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }

        Role role = Role.builder()
                .name(name)
                .description(request.getDescription())
                .build();

        // Optional immediate permission attachment by ID.
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (Long permissionId : request.getPermissionIds()) {
                Permission permission = permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Permission not found: " + permissionId));
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        return toDto(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDto getRole(Long id) {
        return roleRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
    }

    @Transactional
    public RoleDto addPermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

        role.getPermissions().add(permission);
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public RoleDto removePermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        try {
            roleRepository.delete(role);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Cannot delete role " + id + " - it is still assigned to one or more users", ex
            );
        }
    }

    private RoleDto toDto(Role role) {
        Set<PermissionDto> permissionDtos = role.getPermissions().stream()
                .map(p -> PermissionDto.builder()
                        .id(p.getId())
                        .resource(p.getResource())
                        .action(p.getAction())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toSet());

        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionDtos)
                .build();
    }
}