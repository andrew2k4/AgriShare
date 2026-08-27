package com.gaming.agricul.Controller;

import com.google.firebase.auth.FirebaseToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Firebase authentication")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's info from the Firebase token",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        FirebaseToken token = (FirebaseToken) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "uid", token.getUid(),
                "email", token.getEmail() != null ? token.getEmail() : "",
                "name", token.getName() != null ? token.getName() : "",
                "picture", token.getPicture() != null ? token.getPicture() : ""
        ));
    }
}
