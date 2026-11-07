package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.IdentityProvider;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserIdentity;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.repository.AppUserRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.UserIdentityRepository;
import com.gaming.agricul.security.CredentialRules;
import com.gaming.agricul.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Finds or creates the local account behind a Google sign-in. Google only proves who the
 * person is; the account, its role and its session stay ours.
 *
 * <p>Accounts are matched on Google's stable "sub" id once linked, never on the e-mail
 * (an address can change hands). Linking by e-mail happens once per account, and only when
 * Google says the address is verified:
 * <ul>
 *   <li>no account has the address: a new account is created;</li>
 *   <li>the account has a verified address too: it is linked, unless it is already linked to
 *       another Google account (the address now belongs to someone else);</li>
 *   <li>the account never proved it owns the address (an unconfirmed registration, or a
 *       former Firebase account whose address was not verified): Google just proved it, so
 *       the account goes to the Google user. Its password is removed and its sessions end, so
 *       whoever created it with someone else's address loses access.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GoogleLoginService {

    /**
     * The signed-in account. When {@code revokeExistingSessions} is true the caller must end
     * the account's other sessions; it is done after this transaction commits, so a failed
     * login cannot sign anyone out.
     */
    public record Result(AppUser user, boolean revokeExistingSessions) {}

    private final AppUserRepository appUserRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final ProjectRepository projectRepository;

    public Result login(String subject, String rawEmail, boolean emailVerified, String fullName) {
        Optional<UserIdentity> linked = userIdentityRepository.findByProviderAndSubject(IdentityProvider.GOOGLE, subject);
        if (linked.isPresent()) {
            AppUser user = appUserRepository.findById(linked.get().getUserId())
                    .orElseThrow(() -> new IllegalStateException("Google identity linked to a missing user"));
            return new Result(signedIn(user), false);
        }

        String email = CredentialRules.lowerCaseProviderEmail(rawEmail);
        if (email == null || !emailVerified) {
            throw new AuthException(HttpStatus.FORBIDDEN, "GOOGLE_EMAIL_NOT_VERIFIED",
                    "Your Google account's e-mail address is not verified");
        }

        Optional<AppUser> holder = appUserRepository.findByEmail(email);
        AppUser user;
        boolean revoke = false;
        if (holder.isEmpty()) {
            user = AppUser.create(email, CredentialRules.cleanDisplayName(fullName, email), Role.INVESTOR, UserStatus.ACTIVE);
            user.setEmailVerifiedAt(Instant.now());
            user = appUserRepository.save(user);
            log.info("Created account {} from a Google sign-in", user.getId());
        } else {
            user = holder.get();
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw suspended();
            }
            boolean linkedToAnotherGoogleAccount = userIdentityRepository.findByUserId(user.getId()).stream()
                    .anyMatch(identity -> identity.getProvider() == IdentityProvider.GOOGLE);
            if (linkedToAnotherGoogleAccount) {
                log.warn("Google sub {} has the e-mail of account {}, already linked to another Google account",
                        subject, user.getId());
                throw new AuthException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH",
                        "This e-mail belongs to an account linked to a different Google account");
            }
            if (!user.isEmailVerified()) {
                revoke = takeOver(user, fullName);
            }
            log.info("Linked Google identity to account {}", user.getId());
        }

        UserIdentity identity = new UserIdentity();
        identity.setUserId(user.getId());
        identity.setProvider(IdentityProvider.GOOGLE);
        identity.setSubject(subject);
        identity.setEmail(email);
        identity.setCreatedAt(Instant.now());
        userIdentityRepository.save(identity);
        return new Result(signedIn(user), revoke);
    }

    /** Returns true when someone else could hold a session of this account. */
    private boolean takeOver(AppUser user, String fullName) {
        log.warn("Account {} never proved it owns its e-mail; handing it to the verified Google user", user.getId());
        boolean someoneElseCouldSignIn = user.hasPassword();
        user.setPasswordHash(null);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setEmailVerifiedAt(Instant.now());
        if (user.getFirebaseUid() != null) {
            // Former Firebase account: its projects move to the key of the local account, so
            // nothing is left keyed by the old Firebase UID.
            String newOwner = CurrentUserResolver.ownerKey(withoutFirebase(user));
            int moved = projectRepository.reassignOwner(user.getFirebaseUid(), newOwner);
            log.info("Detached Firebase UID from account {}, {} project(s) moved to {}", user.getId(), moved, newOwner);
            user.setFirebaseUid(null);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            // An unconfirmed registration: its username and name were chosen by whoever filled
            // in the form, possibly not the owner of the address
            user.setUsername(null);
            user.setDisplayName(CredentialRules.cleanDisplayName(fullName, user.getEmail()));
        }
        return someoneElseCouldSignIn;
    }

    /** Google proves the address, so an account still waiting for confirmation becomes active. */
    private AppUser signedIn(AppUser user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw suspended();
        }
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }
        user.setLastLoginAt(Instant.now());
        // Managed entity: written by dirty checking when the transaction commits
        return user;
    }

    private static AppUser withoutFirebase(AppUser user) {
        AppUser copy = new AppUser();
        copy.setId(user.getId());
        return copy;
    }

    private static AuthException suspended() {
        return new AuthException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account is suspended");
    }
}
