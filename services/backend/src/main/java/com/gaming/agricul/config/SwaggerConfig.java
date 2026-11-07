package com.gaming.agricul.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI(@Value("${app.cookie.secure:true}") boolean secureCookies) {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("session", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                // Same rule as SecurityConfig.cookieName
                                .name(secureCookies ? "__Host-SESSION" : "SESSION")
                                .description("Session cookie set by POST /api/auth/login. State-changing "
                                        + "requests also need the X-XSRF-TOKEN header from GET /api/auth/csrf.")));
    }
}
