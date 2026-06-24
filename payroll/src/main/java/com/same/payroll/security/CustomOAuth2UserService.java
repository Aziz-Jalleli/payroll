package com.same.payroll.security;

import com.same.payroll.entity.Employee;
import com.same.payroll.entity.User;
import com.same.payroll.exception.EmployeeNotFoundException;
import com.same.payroll.repository.EmployeeRepository;
import com.same.payroll.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Invoked by Spring Security after Google has authenticated the user and
 * returned their profile. This is the ONLY place new User rows get created -
 * registration is implicit and gated entirely on the employees table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegates to Spring's default implementation to actually call Google's
        // userinfo endpoint and parse the response.
        OAuth2User googleUser = super.loadUser(userRequest);


        String email = googleUser.getAttribute("email");
        Boolean emailVerified = googleUser.getAttribute("email_verified");
        String name = googleUser.getAttribute("name");
        String picture = googleUser.getAttribute("picture");
        String googleId = googleUser.getAttribute("sub");
        log.info("OAuth2 login attempt for email: {}, verified: {}", email, emailVerified);


        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account has no email");
        }
        if (emailVerified != null && !emailVerified) {
            throw new OAuth2AuthenticationException("Google email is not verified");
        }

        User user = resolveUser(email, name, googleId, picture);
        return new CustomUserPrincipal(user, googleUser.getAttributes());
    }

    /**
     * Shared core of the login flow: gate on the employees table, then
     * find-or-create the User row. Used by the real Google OAuth2 flow
     * above AND by the dev-only test login endpoint, so both paths stay
     * identical in behavior.
     */
    @Transactional
    public User resolveUser(String email, String name, String googleId, String picture) {
        // --- Gate registration on the employees table ---
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Rejected login attempt - no employee found for email {}", email);
                    return new EmployeeNotFoundException(email);
                });

        if (employee.getStatus() != Employee.EmployeeStatus.ACTIVE) {
            throw new OAuth2AuthenticationException(
                    "Employee account is not active (status: " + employee.getStatus() + ")"
            );
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("First login for employee {} - creating user account", email);
            User newUser = User.builder()
                    .email(email)
                    .fullName(name != null ? name : employee.getFullName())
                    .googleId(googleId)
                    .pictureUrl(picture)
                    .employee(employee)
                    .enabled(true)
                    .build();
            // New users start with zero roles - an admin must assign roles
            // before this account can do anything beyond authenticate.
            return userRepository.save(newUser);
        });

        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException("User account is disabled");
        }

        // Keep googleId/picture fresh and stamp last login.
        user.setGoogleId(googleId);
        user.setPictureUrl(picture);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}