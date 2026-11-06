package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A signed-in device or browser of the current user")
public record SessionDto(
        @Schema(description = "Opaque handle used to revoke this session (not the session id)")
        String handle,
        Instant createdAt,
        Instant lastAccessedAt,
        @Schema(description = "IP address at login")
        String ipAddress,
        @Schema(description = "Browser user agent at login")
        String userAgent,
        @Schema(description = "True for the session making this request")
        boolean current
) {}
