package com.gaming.agricul.Controller;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.security.CurrentUserResolver;
import com.google.firebase.auth.FirebaseToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Firebase authentication")
public class AuthController {

    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's info from the Firebase token, including their role",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        AppUser appUser = currentUserResolver.resolve(authentication);
        FirebaseToken token = authentication != null && authentication.getPrincipal() instanceof FirebaseToken t
                ? t : null;
        // LinkedHashMap, not Map.of: firebaseUid is null for accounts created without Firebase
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", appUser.getId());
        body.put("uid", appUser.getFirebaseUid());
        body.put("email", token != null && token.getEmail() != null ? token.getEmail()
                : Objects.requireNonNullElse(appUser.getEmail(), ""));
        body.put("name", token != null && token.getName() != null ? token.getName()
                : Objects.requireNonNullElse(appUser.getDisplayName(), ""));
        body.put("picture", token != null && token.getPicture() != null ? token.getPicture() : "");
        body.put("role", appUser.getRole());
        return ResponseEntity.ok(body);
    }
}
