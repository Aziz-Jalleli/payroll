package com.same.payroll.exception;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * Thrown during the OAuth2 user-loading step when the authenticated Google
 * account's email does not match any row in the employees table.
 * Spring Security will redirect to /login?error and the reason can be
 * surfaced via the AuthenticationFailureHandler.
 */
public class EmployeeNotFoundException extends OAuth2AuthenticationException {

    public EmployeeNotFoundException(String email) {
        super(
                new OAuth2Error(
                        OAuth2ErrorCodes.ACCESS_DENIED,
                        "No employee record found for email: " + email,
                        null
                )
        );
    }
}