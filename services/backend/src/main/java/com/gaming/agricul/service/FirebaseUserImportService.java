package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.repository.AppUserRepository;
import com.gaming.agricul.security.CredentialRules;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Copies every Firebase account into app_users, so that each one exists on our side before
 * Firebase goes away. Until now an account was only created at its first API call, so some
 * Firebase users have no row at all.
 *
 * <p>Idempotent: run it as often as needed. It never creates duplicates, never removes
 * anything and never touches passwords (those migrate at each user's first password login).
 * <ul>
 *   <li>anonymous accounts (no sign-in method) are skipped;</li>
 *   <li>"e-mail verified" is copied when Firebase has it and we do not: the Google sign-in
 *       relies on it to decide whether an account proved it owns its address;</li>
 *   <li>the e-mail is updated when it changed in Firebase (if no other account has it);</li>
 *   <li>accounts disabled in Firebase are suspended when the import creates them; existing
 *       ones are only reported, since an admin may have decided otherwise here.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseUserImportService {

    /**
     * @param createdWithoutEmail   Firebase e-mail already used by another account: merge by hand
     * @param disabledInFirebaseOnly existing accounts disabled in Firebase but active here; left
     *                               alone (an admin may have reactivated them), to review by hand
     */
    public record Report(int created, int updated, int unchanged, int skippedAnonymous, int createdWithoutEmail,
                         int disabledInFirebaseOnly) {}

    private final FirebaseAuth firebaseAuth;
    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;

    public Report importAll() throws FirebaseAuthException {
        int created = 0, updated = 0, unchanged = 0, anonymous = 0, withoutEmail = 0, disabledOnly = 0;
        for (ExportedUserRecord record : firebaseAuth.listUsers(null).iterateAll()) {
            if (record.getProviderData().length == 0) {
                anonymous++;
                continue;
            }
            Optional<AppUser> existing = appUserRepository.findByFirebaseUid(record.getUid());
            if (existing.isEmpty()) {
                AppUser user = appUserService.findOrCreateByUid(
                        record.getUid(), record.getEmail(), record.getDisplayName(), record.isEmailVerified());
                if (record.getEmail() != null && user.getEmail() == null) {
                    withoutEmail++;
                }
                suspendIfDisabled(user, record);
                created++;
            } else {
                if (sync(existing.get(), record)) {
                    updated++;
                } else {
                    unchanged++;
                }
                if (record.isDisabled() && existing.get().getStatus() == UserStatus.ACTIVE) {
                    disabledOnly++;
                    log.warn("Account {} is disabled in Firebase but active here: review it", existing.get().getId());
                }
            }
        }
        Report report = new Report(created, updated, unchanged, anonymous, withoutEmail, disabledOnly);
        log.info("Firebase import: {}", report);
        return report;
    }

    /**
     * Copies what Firebase knows better: the current e-mail (users can change it there, and the
     * password migration signs in with it) and its verified state. Returns true when something
     * changed. The status is not touched: see Report.disabledInFirebaseOnly.
     */
    private boolean sync(AppUser user, ExportedUserRecord record) {
        boolean changed = false;
        String firebaseEmail = CredentialRules.lowerCaseProviderEmail(record.getEmail());
        if (firebaseEmail != null && !firebaseEmail.equals(user.getEmail())) {
            if (appUserRepository.existsByEmail(firebaseEmail)) {
                log.warn("Firebase e-mail of account {} is used by another account: not copied, merge by hand", user.getId());
            } else {
                user.setEmail(firebaseEmail);
                user.setEmailVerifiedAt(null);
                changed = true;
            }
        }
        if (record.isEmailVerified() && user.getEmailVerifiedAt() == null
                && user.getEmail() != null && Objects.equals(user.getEmail(), firebaseEmail)) {
            user.setEmailVerifiedAt(Instant.now());
            changed = true;
        }
        if (changed) {
            appUserRepository.save(user);
        }
        return changed;
    }

    /** Only for accounts created by this import: they have no session and no admin decision yet. */
    private void suspendIfDisabled(AppUser user, ExportedUserRecord record) {
        if (record.isDisabled() && user.getStatus() != UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.SUSPENDED);
            appUserRepository.save(user);
        }
    }
}
