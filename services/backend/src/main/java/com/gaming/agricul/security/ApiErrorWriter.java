package com.gaming.agricul.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * JSON errors for the security layer, same shape as GlobalExceptionHandler's, so the frontend
 * can tell "not signed in" (401) from "not allowed" (403) and a CSRF failure (403, code CSRF).
 */
public final class ApiErrorWriter {

    private ApiErrorWriter() {
    }

    public static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) ->
                write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, ex) -> {
            if (ex instanceof CsrfException) {
                write(response, HttpStatus.FORBIDDEN, "CSRF", "Missing or invalid CSRF token");
            } else {
                write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
            }
        };
    }

    /** Messages and codes are our own constants: no escaping of user input is needed. */
    public static void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message
                + "\",\"code\":\"" + code + "\"}");
    }
}
