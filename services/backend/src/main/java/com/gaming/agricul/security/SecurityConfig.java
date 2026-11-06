package com.gaming.agricul.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    /**
     * Only "true" and "false" select a filter chain below. Any other value (e.g. "yes",
     * "ture") would match neither and leave Spring Boot's default chain in place, so it is
     * rejected at startup instead.
     */
    public SecurityConfig(@Value("${firebase.enabled:true}") String firebaseEnabled) {
        if (!"true".equalsIgnoreCase(firebaseEnabled.trim()) && !"false".equalsIgnoreCase(firebaseEnabled.trim())) {
            throw new IllegalStateException(
                    "firebase.enabled must be 'true' or 'false', got '" + firebaseEnabled + "'");
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain firebaseFilterChain(HttpSecurity http, FirebaseTokenFilter firebaseTokenFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Insecure dev chain: every request is let through and runs as the ADMIN "dev-user".
     * Only active when firebase.enabled is explicitly set to false. A missing setting falls
     * back to the secured chain above (fail closed); any other value stops the startup.
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "false")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        log.warn("SECURITY DISABLED: firebase.enabled=false, every request runs as the ADMIN dev-user. "
                + "Never use this setting outside local development.");
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
