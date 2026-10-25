package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.service.AppUserService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the local {@link AppUser} behind the current request, auto-provisioning it
 * on first login. Falls back to a fixed "dev-user" (ADMIN) identity when Firebase auth
 * is disabled, mirroring the existing dev-mode convention used across controllers.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final AppUserService appUserService;

    public AppUser resolve(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof FirebaseToken token) {
            return appUserService.findOrCreateByUid(token.getUid(), token.getEmail(), token.getName());
        }
        return appUserService.findOrCreateByUid("dev-user", null, "Dev User");
    }
}
