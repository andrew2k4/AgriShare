package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.repository.AppUserRepository;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Runs after the session (or Firebase token) has been read:
 * <ul>
 *   <li>Session users are reloaded from the database. A deleted, suspended or not-yet-active
 *       account, or a session older than the absolute limit, is signed out (the session is
 *       deleted and the request continues unauthenticated). Otherwise the request runs with
 *       the user's current role. The stored session is not modified.</li>
 *   <li>Firebase users whose account was suspended get a 403.</li>
 * </ul>
 */
public class SessionUserFilter extends OncePerRequestFilter {

    /** Epoch millis of the login, stored in the session. */
    public static final String AUTH_TIME = "agrishare.authTime";
    public static final String CLIENT_IP = "agrishare.ip";
    public static final String USER_AGENT = "agrishare.userAgent";

    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
    private final AppUserRepository appUserRepository;
    private final Duration absoluteTimeout;

    public SessionUserFilter(AppUserRepository appUserRepository, Duration absoluteTimeout) {
        this.appUserRepository = appUserRepository;
        this.absoluteTimeout = absoluteTimeout;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = holder.getContext().getAuthentication();

        if (authentication instanceof SessionAuthentication stored) {
            HttpSession session = request.getSession(false);
            Optional<AppUser> user = appUserRepository.findById(stored.getUserId());
            if (session == null || isExpired(session.getAttribute(AUTH_TIME), Instant.now(), absoluteTimeout)
                    || user.isEmpty() || user.get().getStatus() != UserStatus.ACTIVE) {
                if (session != null) {
                    session.invalidate();
                }
                holder.clearContext();
            } else {
                SecurityContext context = holder.createEmptyContext();
                context.setAuthentication(SessionAuthentication.withRole(user.get().getId(), user.get().getRole()));
                holder.setContext(context);
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof FirebaseToken token) {
            boolean suspended = appUserRepository.findByFirebaseUid(token.getUid())
                    .map(u -> u.getStatus() == UserStatus.SUSPENDED)
                    .orElse(false);
            if (suspended) {
                holder.clearContext();
                ApiErrorWriter.write(response, HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account is suspended");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    static boolean isExpired(Object authTime, Instant now, Duration absoluteTimeout) {
        if (!(authTime instanceof Long millis)) {
            return true;
        }
        return Instant.ofEpochMilli(millis).plus(absoluteTimeout).isBefore(now);
    }
}
