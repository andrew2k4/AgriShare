package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/** Opens the server-side session of a signed-in user, the same way for password and Google logins. */
@Component
@RequiredArgsConstructor
public class SessionLogin {

    private static final int USER_AGENT_MAX_LENGTH = 255;

    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final AdminBootstrap adminBootstrap;

    public void open(AppUser user, HttpServletRequest request, HttpServletResponse response) {
        adminBootstrap.apply(user);

        // New session id at login (session fixation): drop whatever session existed before
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionUserFilter.AUTH_TIME, System.currentTimeMillis());
        session.setAttribute(SessionUserFilter.CLIENT_IP, ClientIp.of(request));
        session.setAttribute(SessionUserFilter.USER_AGENT, truncate(request.getHeader(HttpHeaders.USER_AGENT)));

        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(SessionAuthentication.stored(user.getId()));
        holder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // A token obtained before login is not kept for the signed-in session: the frontend
        // must fetch a new one from /api/auth/csrf after logging in
        csrfTokenRepository.saveToken(null, request, response);
    }

    /** Ends the current session, if any. */
    public void close(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        holder.clearContext();
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > USER_AGENT_MAX_LENGTH ? value.substring(0, USER_AGENT_MAX_LENGTH) : value;
    }
}
