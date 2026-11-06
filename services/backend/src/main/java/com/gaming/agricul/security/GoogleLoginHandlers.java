package com.gaming.agricul.security;

import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.service.GoogleLoginService;
import com.gaming.agricul.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * End of the Google sign-in. Spring Security has checked the callback (state, PKCE, ID token);
 * here the local account is found or created and our own session is opened, exactly as for a
 * password login. The browser is then sent back to the frontend:
 * <ul>
 *   <li>success: {frontend}/auth/callback</li>
 *   <li>failure: {frontend}/login?error=CODE (e.g. GOOGLE_EMAIL_NOT_VERIFIED,
 *       ACCOUNT_SUSPENDED, GOOGLE_ACCOUNT_MISMATCH, GOOGLE_LOGIN_FAILED when the user
 *       cancels on Google's page or anything unexpected happens)</li>
 * </ul>
 * A failure leaves any existing session alone: the callback URL can be opened from any site
 * (it is a plain GET), so signing the user out there would let other sites log people out.
 * The redirect targets are fixed: no "return to" parameter, so this cannot be used as an
 * open redirect.
 */
@Slf4j
@Component
public class GoogleLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final GoogleLoginService googleLoginService;
    private final SessionService sessionService;
    private final SessionLogin sessionLogin;
    private final String frontendUrl;

    public GoogleLoginHandlers(GoogleLoginService googleLoginService, SessionService sessionService,
                               SessionLogin sessionLogin, @Value("${app.frontend-url}") String frontendUrl) {
        this.googleLoginService = googleLoginService;
        this.sessionService = sessionService;
        this.sessionLogin = sessionLogin;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser google)) {
            fail(response, "GOOGLE_LOGIN_FAILED");
            return;
        }
        GoogleLoginService.Result result;
        try {
            result = googleLoginService.login(google.getSubject(), google.getEmail(),
                    Boolean.TRUE.equals(google.getEmailVerified()), google.getFullName());
        } catch (AuthException e) {
            fail(response, e.getCode());
            return;
        } catch (RuntimeException e) {
            // e.g. two first sign-ins of the same new user at once hitting a unique index
            log.error("Google sign-in failed unexpectedly", e);
            fail(response, "GOOGLE_LOGIN_FAILED");
            return;
        }
        // The account changed hands: end the sessions of whoever held it before
        if (result.revokeExistingSessions()) {
            sessionService.revokeAll(result.user().getId());
        }
        sessionLogin.open(result.user(), request, response);
        response.sendRedirect(frontendUrl + "/auth/callback");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException {
        log.info("Google sign-in failed: {}", exception.getMessage());
        fail(response, "GOOGLE_LOGIN_FAILED");
    }

    private void fail(HttpServletResponse response, String code) throws IOException {
        // Only this request's context (Spring's Google token) is dropped, not the session
        SecurityContextHolder.clearContext();
        response.sendRedirect(frontendUrl + "/login?error=" + code);
    }
}
