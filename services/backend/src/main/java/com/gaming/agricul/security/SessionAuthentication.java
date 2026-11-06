package com.gaming.agricul.security;

import com.gaming.agricul.Role;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Authentication of a user signed in through the backend (password or Google) and kept in a
 * server-side session. Only the user id is stored in the session: the role and status are
 * reloaded from the database on every request (see {@link SessionUserFilter}), so a role
 * change or a suspension applies immediately.
 *
 * <p>{@link #getName()} is the user id: Spring Session indexes sessions by it, which is how
 * all sessions of one user are listed and revoked.
 */
public class SessionAuthentication extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;

    private SessionAuthentication(Long userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        setAuthenticated(true);
    }

    /** What is stored in the session at login: the id only. */
    public static SessionAuthentication stored(Long userId) {
        return new SessionAuthentication(userId, List.of());
    }

    /** What a request runs with, built from the freshly loaded role. */
    public static SessionAuthentication withRole(Long userId, Role role) {
        return new SessionAuthentication(userId, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
