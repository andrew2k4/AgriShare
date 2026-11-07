package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request bodies of /api/auth. */
public final class AuthRequests {

    private AuthRequests() {
    }

    @Schema(description = "Sign in with an e-mail or a username")
    public record Login(
            @Schema(description = "E-mail address or username", example = "marie@example.com") String identifier,
            String password) {}

    @Schema(description = "Create an account; a confirmation e-mail is sent")
    public record Register(
            @Schema(example = "marie@example.com") String email,
            @Schema(description = "3-30 characters: letters, digits, '.', '_', '-'", example = "marie.curie") String username,
            @Schema(description = "8 to 128 characters") String password,
            @Schema(description = "Optional, defaults to the username", example = "Marie Curie") String displayName) {}

    public record Email(@Schema(example = "marie@example.com") String email) {}

    public record Token(@Schema(description = "Token from the e-mailed link") String token) {}

    public record ResetPassword(String token, String newPassword) {}

    public record ChangePassword(String currentPassword, String newPassword) {}

    public record Username(@Schema(example = "marie.curie") String username) {}
}
