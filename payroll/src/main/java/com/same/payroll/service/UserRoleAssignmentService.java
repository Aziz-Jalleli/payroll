package com.same.payroll.service;

import com.same.payroll.dto.AdminUserDto;
import com.same.payroll.entity.Role;
import com.same.payroll.entity.User;
import com.same.payroll.repository.RoleRepository;
import com.same.payroll.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public AdminUserDto assignRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Set<Role> roles = roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name)))
                .collect(Collectors.toSet());

        user.setRoles(new HashSet<>(roles));
        return toDto(userRepository.save(user));
    }

    /**
     * Replaces a user's roles with a single role - convenience method for
     * the admin UI's role dropdown, which shows/sets exactly one role per
     * user rather than a multi-select.
     */
    @Transactional
    public AdminUserDto setSingleRole(Long userId, String roleName) {
        return assignRoles(userId, Set.of(roleName));
    }

    @Transactional
    public AdminUserDto addRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        user.getRoles().add(role);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto removeRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.getRoles().removeIf(r -> r.getName().equalsIgnoreCase(roleName));
        return toDto(userRepository.save(user));
    }

    private AdminUserDto toDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}