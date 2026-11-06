package com.gaming.agricul.security;

import com.gaming.agricul.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Two ways to authenticate coexist until the frontend leaves Firebase:
 * <ul>
 *   <li>a Firebase ID token in "Authorization: Bearer" (stateless, as before);</li>
 *   <li>a server-side session (cookie) opened by /api/auth/login.</li>
 * </ul>
 *
 * <p>CSRF protection (double submit: a cookie plus the X-XSRF-TOKEN header, token from
 * GET /api/auth/csrf) applies to every state-changing request except Bearer ones: a page
 * on another site cannot add an Authorization header without passing CORS, which only
 * allows our own origins.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Reachable without being signed in. */
    static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/csrf",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/auth/password/forgot",
            "/api/auth/password/reset",
            "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html"
    };

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Value("${app.session.absolute-timeout:12h}")
    private Duration absoluteSessionTimeout;

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
        // Not a CORS-safelisted header: without this the frontend cannot read the 429 delay
        config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Same repository for the filter chain (reading) and the login endpoint (saving). */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
    }

    /**
     * Session cookie: HttpOnly, SameSite=Lax, and with the "__Host-" prefix when Secure, which
     * makes the browser refuse a cookie of that name set by another subdomain or without
     * Secure. Not shared with other subdomains (no Domain attribute).
     */
    @Bean
    public CookieSerializer cookieSerializer(@Value("${app.cookie.secure:true}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName("SESSION", secure));
        serializer.setUseSecureCookie(secure);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        return serializer;
    }

    /** HttpOnly as well: the frontend reads the token from GET /api/auth/csrf, not the cookie. */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(@Value("${app.cookie.secure:true}") boolean secure) {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName(cookieName("XSRF-TOKEN", secure));
        repository.setCookieCustomizer(cookie -> cookie.secure(secure).httpOnly(true).sameSite("Lax").path("/"));
        return repository;
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain firebaseFilterChain(HttpSecurity http, FirebaseTokenFilter firebaseTokenFilter,
                                                   AppUserRepository appUserRepository,
                                                   CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        common(http, appUserRepository, csrfTokenRepository)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Insecure dev chain: every request is let through, and one without a session runs as the
     * ADMIN "dev-user". Only active when firebase.enabled is explicitly set to false. A missing
     * setting falls back to the secured chain above (fail closed); any other value stops the
     * startup.
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "false")
    public SecurityFilterChain devFilterChain(HttpSecurity http, AppUserRepository appUserRepository,
                                              CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        log.warn("SECURITY DISABLED: firebase.enabled=false, every request without a session runs as the "
                + "ADMIN dev-user. Never use this setting outside local development.");
        common(http, appUserRepository, csrfTokenRepository)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * The SessionUserFilter is created here rather than as a bean: Spring Boot registers every
     * Filter bean as a global servlet filter too, outside the security chain.
     */
    private HttpSecurity common(HttpSecurity http, AppUserRepository appUserRepository,
                                CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        SessionUserFilter sessionUserFilter = new SessionUserFilter(appUserRepository, absoluteSessionTimeout);
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // Plain handler: the header carries the raw token returned by /api/auth/csrf
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(csrfRequired()))
                .securityContext(context -> context.securityContextRepository(securityContextRepository()))
                // Never store the request of an anonymous caller: that would create a session
                // (a database row) for every unauthenticated hit.
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(ApiErrorWriter.unauthorized())
                        .accessDeniedHandler(ApiErrorWriter.forbidden()))
                .addFilterAfter(sessionUserFilter, UsernamePasswordAuthenticationFilter.class);
    }

    static RequestMatcher csrfRequired() {
        return request -> {
            if (SAFE_METHODS.contains(request.getMethod())) {
                return false;
            }
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            return authorization == null || !authorization.startsWith("Bearer ");
        };
    }

    private static String cookieName(String base, boolean secure) {
        // Browsers only accept "__Host-" cookies that are Secure
        return secure ? "__Host-" + base : base;
    }
}
