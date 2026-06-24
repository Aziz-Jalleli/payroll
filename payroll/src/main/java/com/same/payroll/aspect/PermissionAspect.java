package com.same.payroll.aspect;

import com.same.payroll.annotation.RequirePermission;
import com.same.payroll.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        if (permissionService.getCurrentPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }

        if (!permissionService.hasPermission(requirePermission.resource(), requirePermission.action())) {
            throw new AccessDeniedException(
                    "Insufficient permissions: requires "
                            + requirePermission.resource() + ":" + requirePermission.action()
            );
        }
    }
}