package com.gaming.agricul.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.time.Duration;

/**
 * Starting a Google sign-in stores the pending request in a session, i.e. a database row
 * that lives until the idle timeout. Capping how often one IP can start the flow keeps an
 * anonymous script from filling the sessions table. Over the limit the request is not
 * treated as a sign-in start (no redirect to Google, nothing stored).
 */
class RateLimitedAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final int STARTS_PER_IP = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final OAuth2AuthorizationRequestResolver delegate;
    private final RateLimiter rateLimiter;

    RateLimitedAuthorizationRequestResolver(OAuth2AuthorizationRequestResolver delegate, RateLimiter rateLimiter) {
        this.delegate = delegate;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
        return allowed(authorizationRequest, request) ? authorizationRequest : null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request, clientRegistrationId);
        return allowed(authorizationRequest, request) ? authorizationRequest : null;
    }

    private boolean allowed(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        return authorizationRequest == null
                || rateLimiter.tryAcquire("google-start:" + ClientIp.of(request), STARTS_PER_IP, WINDOW);
    }
}
