package com.gaming.agricul.Controller;

import com.gaming.agricul.Role;
import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.security.CurrentUserResolver;
import com.gaming.agricul.service.FirebaseUserImportService;
import com.google.firebase.auth.FirebaseAuthException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/firebase")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Firebase migration", description = "Moving existing Firebase accounts to the backend")
public class FirebaseMigrationController {

    private final FirebaseUserImportService importService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/import")
    @Operation(summary = "Create the missing app_users rows from Firebase and copy their verified state (Admin only, idempotent)")
    public FirebaseUserImportService.Report importUsers(Authentication authentication) throws FirebaseAuthException {
        if (currentUserResolver.resolve(authentication).getRole() != Role.ADMIN) {
            throw new AuthException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This action is reserved to administrators");
        }
        return importService.importAll();
    }
}
