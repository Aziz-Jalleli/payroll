package com.same.payroll.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirects failed OAuth2 logins to the frontend with a readable error
 * reason instead of Spring's default error page. EmployeeNotFoundException
 * (and any OAuth2AuthenticationException) lands here.
 */
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.error-redirect:http://localhost:5173/auth/error}")
    private String errorRedirect;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String message;

        if (exception instanceof OAuth2AuthenticationException oae
                && oae.getError() != null) {
            message = oae.getError().getDescription() != null
                    ? oae.getError().getDescription()
                    : "Authentication failed";
        } else {
            message = "Authentication failed";
        }

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(
                request,
                response,
                errorRedirect + "?error=" + encoded
        );
    }
}