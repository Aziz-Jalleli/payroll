package com.same.payroll.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place on any controller or service method that requires the current
 * authenticated user to hold a permission matching (resource, action).
 *
 * Example: @RequirePermission(resource = "EMPLOYEE", action = "READ")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequirePermission {
    String resource();
    String action();
}