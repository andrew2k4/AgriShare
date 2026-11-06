package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.service.AppUserService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the local {@link AppUser} behind the current request:
 * <ul>
 *   <li>a backend session (password or Google login): the user it belongs to;</li>
 *   <li>a Firebase token: the matching user, auto-provisioned on first login;</li>
 *   <li>otherwise the fixed ADMIN "dev-user". This only happens with the insecure dev
 *       chain (firebase.enabled=false): the secured chain rejects unauthenticated requests
 *       before they reach a controller.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final AppUserService appUserService;

    public AppUser resolve(Authentication authentication) {
        if (authentication instanceof SessionAuthentication session) {
            return appUserService.findById(session.getUserId());
        }
        if (authentication != null && authentication.getPrincipal() instanceof FirebaseToken token) {
            return appUserService.findOrCreateByUid(
                    token.getUid(), token.getEmail(), token.getName(), token.isEmailVerified());
        }
        return appUserService.findOrCreateByUid("dev-user", null, "Dev User", false);
    }

    /**
     * The value stored in projects.owner_uid for this user. Accounts that came from Firebase
     * keep their Firebase UID, so the projects they already own still match; accounts created
     * in the backend use a key built from their id, which can never collide with a Firebase UID.
     */
    public static String ownerKey(AppUser user) {
        return user.getFirebaseUid() != null ? user.getFirebaseUid() : "app-user:" + user.getId();
    }
}
