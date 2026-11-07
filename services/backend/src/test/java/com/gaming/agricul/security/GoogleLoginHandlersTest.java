package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.service.GoogleLoginService;
import com.gaming.agricul.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleLoginHandlersTest {

    private final GoogleLoginService googleLoginService = mock(GoogleLoginService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final SessionLogin sessionLogin =
            new SessionLogin(new HttpSessionSecurityContextRepository(), new CookieCsrfTokenRepository(),
                    new AdminBootstrap("", null));
    private final GoogleLoginHandlers handlers =
            new GoogleLoginHandlers(googleLoginService, sessionService, sessionLogin, "https://app.agrishare.farm/");

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void success_opensOurOwnSession_andGoesBackToTheFrontend() throws Exception {
        AppUser user = AppUser.create("marie@example.com", "Marie", Role.INVESTOR, UserStatus.ACTIVE);
        user.setId(42L);
        when(googleLoginService.login(eq("sub-1"), eq("marie@example.com"), eq(true), anyString()))
                .thenReturn(new GoogleLoginService.Result(user, false));

        handlers.onAuthenticationSuccess(request, response, googleToken("sub-1", "marie@example.com", true));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.agrishare.farm/auth/callback");
        SecurityContext stored = (SecurityContext) request.getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored.getAuthentication()).isInstanceOf(SessionAuthentication.class);
        assertThat(stored.getAuthentication().getName()).isEqualTo("42");
        assertThat(request.getSession(false).getAttribute(SessionUserFilter.AUTH_TIME)).isInstanceOf(Long.class);
        verify(sessionService, never()).revokeAll(any());
    }

    @Test
    void accountThatChangedHands_hasItsOtherSessionsEnded() throws Exception {
        AppUser user = AppUser.create("v@example.com", "V", Role.INVESTOR, UserStatus.ACTIVE);
        user.setId(43L);
        when(googleLoginService.login(anyString(), anyString(), anyBoolean(), anyString()))
                .thenReturn(new GoogleLoginService.Result(user, true));

        handlers.onAuthenticationSuccess(request, response, googleToken("sub-3", "v@example.com", true));

        verify(sessionService).revokeAll(43L);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.agrishare.farm/auth/callback");
    }

    @Test
    void unexpectedError_stillEndsOnTheFrontend() throws Exception {
        when(googleLoginService.login(anyString(), anyString(), anyBoolean(), anyString()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        handlers.onAuthenticationSuccess(request, response, googleToken("sub-4", "d@example.com", true));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.agrishare.farm/login?error=GOOGLE_LOGIN_FAILED");
    }

    @Test
    void refusedLogin_redirectsWithTheCode_andDoesNotSignOutAnExistingSession() throws Exception {
        when(googleLoginService.login(anyString(), anyString(), anyBoolean(), anyString()))
                .thenThrow(new AuthException(HttpStatus.FORBIDDEN, "GOOGLE_EMAIL_NOT_VERIFIED", "x"));
        var existing = request.getSession(true); // e.g. the user was already signed in

        handlers.onAuthenticationSuccess(request, response, googleToken("sub-2", "a@example.com", false));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.agrishare.farm/login?error=GOOGLE_EMAIL_NOT_VERIFIED");
        assertThat(request.getSession(false)).isSameAs(existing);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonGooglePrincipal_isRefused() throws Exception {
        handlers.onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken("someone", null, List.of()));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.agrishare.farm/login?error=GOOGLE_LOGIN_FAILED");
    }

    @Test
    void failureOnGooglesSide_redirectsWithAGenericCode() throws Exception {
        handlers.onAuthenticationFailure(request, response,
                new org.springframework.security.oauth2.core.OAuth2AuthenticationException("access_denied"));

        assertThat(response.getRedirectedUrl()).isEqualTo("https://app.agrishare.farm/login?error=GOOGLE_LOGIN_FAILED");
    }

    private static OAuth2AuthenticationToken googleToken(String sub, String email, boolean verified) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "email_verified", verified, "name", "Marie Curie");
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60), claims);
        DefaultOidcUser user = new DefaultOidcUser(List.of(new OAuth2UserAuthority(claims)), idToken);
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }
}
