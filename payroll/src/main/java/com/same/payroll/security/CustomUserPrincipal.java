package com.same.payroll.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.same.payroll.entity.Permission;
import com.same.payroll.entity.Role;
import com.same.payroll.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wraps our domain User as a Spring Security OAuth2User.
 * Authorities are derived from the user's roles AND each role's permissions,
 * so both @PreAuthorize("hasRole(...)") style checks and our custom
 * PermissionService checks can work off the same SecurityContext.
 *
 * Stored directly in the HttpSession as the authenticated principal, which
 * with spring-session-data-redis means it gets JSON-serialized via
 * GenericJackson2JsonRedisSerializer (see RedisSessionConfig). That requires:
 *   - @JsonTypeInfo so Jackson can deserialize back to this exact class
 *     rather than a generic Map (GenericJackson2JsonRedisSerializer embeds
 *     an "@class" field for this purpose)
 *   - a @JsonCreator constructor, since there's no default constructor and
 *     the fields are final
 *   - @JsonIgnoreProperties(ignoreUnknown = true) so additive changes to
 *     this class later don't break deserialization of sessions created by
 *     an older version of it
 * Serializable is kept as a defensive fallback in case any code path or
 * future config switches back to the JDK serializer.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomUserPrincipal implements OAuth2User, Serializable {

    private final Long userId;
    private final String email;
    private final String fullName;
    private final Map<String, Object> attributes;
    private final Set<String> roleNames;
    private final Set<String> permissionAuthorities; // "RESOURCE:ACTION"

    public CustomUserPrincipal(User user, Map<String, Object> attributes) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.attributes = attributes;

        this.roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        this.permissionAuthorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * Used by Jackson to reconstruct this object when a session is read
     * back from Redis. Field names must match the JSON property names
     * Jackson writes, which by default are the bean's field names.
     */
    @JsonCreator
    public CustomUserPrincipal(
            @JsonProperty("userId") Long userId,
            @JsonProperty("email") String email,
            @JsonProperty("fullName") String fullName,
            @JsonProperty("attributes") Map<String, Object> attributes,
            @JsonProperty("roleNames") Set<String> roleNames,
            @JsonProperty("permissionAuthorities") Set<String> permissionAuthorities
    ) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.attributes = attributes;
        this.roleNames = roleNames;
        this.permissionAuthorities = permissionAuthorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<String> getRoleNames() {
        return roleNames;
    }

    public Set<String> getPermissionAuthorities() {
        return permissionAuthorities;
    }

    public boolean hasPermission(String resource, String action) {
        return permissionAuthorities.contains(
                resource.toUpperCase() + ":" + action.toUpperCase()
        );
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        roleNames.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissionAuthorities.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }

    @Override
    public String getName() {
        return email;
    }
}