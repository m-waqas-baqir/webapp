package com.app.backend.security;

import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;
    /** Fine-grained permission codes from {@code user_permissions}. */
    private final Set<String> permissionCodes;
    private final boolean active;

    public static UserPrincipal fromUser(User user, Set<String> permissionCodes) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                user.getRole(),
                authorities,
                permissionCodes != null ? Set.copyOf(permissionCodes) : Set.of(),
                user.isActive()
        );
    }

    /** Backward-compatible factory when permission codes are not loaded yet (empty set). */
    public static UserPrincipal fromUser(User user) {
        return fromUser(user, Collections.emptySet());
    }

    private UserPrincipal(
            Long id,
            String email,
            String password,
            String name,
            UserRole role,
            Collection<? extends GrantedAuthority> authorities,
            Set<String> permissionCodes,
            boolean active
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.authorities = authorities;
        this.permissionCodes = permissionCodes;
        this.active = active;
    }

    public boolean hasPermission(String code) {
        return code != null && permissionCodes.contains(code);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
