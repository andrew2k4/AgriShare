package com.gaming.agricul.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * "Sign in with Google", enabled only when GOOGLE_CLIENT_ID is set: without it the app starts
 * normally and the Google endpoints do not exist. Google calls back on
 * {baseUrl}/login/oauth2/code/google, which must be registered in the Google Cloud console.
 */
@Configuration
@ConditionalOnExpression("!'${app.google.client-id:}'.isBlank()")
public class GoogleOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret}") String clientSecret) {
        if (clientSecret.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID is set but GOOGLE_CLIENT_SECRET is empty");
        }
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId.trim())
                .clientSecret(clientSecret.trim())
                .scope("openid", "email", "profile")
                .build());
    }
}
