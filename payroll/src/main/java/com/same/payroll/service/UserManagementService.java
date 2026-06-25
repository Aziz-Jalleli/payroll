package com.same.payroll.service;

import com.same.payroll.dto.AdminUserDto;
import com.same.payroll.dto.UpdateUserRequest;
import com.same.payroll.entity.Role;
import com.same.payroll.entity.User;
import com.same.payroll.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUser(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public AdminUserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setFullName(request.getFullName().trim());
        return toDto(userRepository.save(user));
    }

    /**
     * Deactivating sets enabled=false rather than deleting the row. A
     * deactivated user's Google login still succeeds at the OAuth2 layer
     * (their email still matches an employee), but CustomOAuth2UserService
     * checks user.isEnabled() and rejects the session - so deactivation
     * revokes access without losing the audit trail (who they were, what
     * roles they had, when they were created).
     *
     * Disabling your OWN account is blocked - otherwise an admin could
     * lock themselves out of the admin panel with no one left who can
     * re-enable them.
     */
    @Transactional
    public AdminUserDto setEnabled(Long id, boolean enabled) {
        if (!enabled) {
            guardAgainstSelfAction(id, "deactivate");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setEnabled(enabled);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        guardAgainstSelfAction(id, "delete");
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private void guardAgainstSelfAction(Long targetUserId, String action) {
        Long currentUserId = permissionService.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new IllegalStateException("You cannot " + action + " your own account");
        }
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