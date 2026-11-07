package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.service.AppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Resolves the local {@link AppUser} behind the current request's session. */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final AppUserService appUserService;

    public AppUser resolve(Authentication authentication) {
        if (authentication instanceof SessionAuthentication session) {
            return appUserService.findById(session.getUserId());
        }
        // The security chain rejects unauthenticated requests before they reach a controller
        throw new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
    }

    /**
     * The value stored in projects.owner_uid for this user. Accounts that came from Firebase
     * keep their former Firebase UID, so the projects they already own still match; the others
     * use a key built from their id.
     */
    public static String ownerKey(AppUser user) {
        return user.getFirebaseUid() != null ? user.getFirebaseUid() : "app-user:" + user.getId();
    }
}
