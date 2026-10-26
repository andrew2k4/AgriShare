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

import java.util.Map;

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
        return ResponseEntity.ok(Map.of(
                "uid", appUser.getFirebaseUid(),
                "email", token != null && token.getEmail() != null ? token.getEmail() : "",
                "name", token != null && token.getName() != null ? token.getName() : "",
                "picture", token != null && token.getPicture() != null ? token.getPicture() : "",
                "role", appUser.getRole()
        ));
    }
}
