package com.gaming.agricul.dto;

import com.gaming.agricul.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Application user, linked to a Firebase account, with an assigned role")
public record AppUserDto(

        @Schema(description = "User identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Firebase UID", example = "abc123", accessMode = Schema.AccessMode.READ_ONLY)
        String firebaseUid,

        @Schema(description = "Email address", example = "marie@agrishare.app")
        String email,

        @Schema(description = "Display name", example = "Marie Curie")
        String displayName,

        @Schema(description = "Assigned role", example = "MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
        Role role,

        @Schema(description = "Account creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt
) {}
