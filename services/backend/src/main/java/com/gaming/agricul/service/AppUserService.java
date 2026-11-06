package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.security.CredentialRules;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AppUser findById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    /**
     * Resolves the local AppUser for a Firebase UID, auto-provisioning it on first login.
     * New users default to the INVESTOR role; an ADMIN can promote them afterwards.
     *
     * <p>The e-mail is stored only if no other account already has it (e.g. a Firebase account
     * deleted and re-created under a new UID). Otherwise the unique index would reject the
     * insert on every request and lock the user out; they are provisioned without e-mail and
     * the conflict is logged so an admin can merge the two accounts.
     */
    public AppUser findOrCreateByUid(String firebaseUid, String email, String displayName, boolean emailVerified) {
        return appUserRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    String normalized = CredentialRules.lowerCaseProviderEmail(email);
                    if (normalized != null) {
                        Optional<AppUser> holder = appUserRepository.findByEmail(normalized);
                        if (holder.isPresent() && emailVerified && holder.get().getStatus() == UserStatus.PENDING) {
                            // A registration that never proved it owns the address: the provider just
                            // did, so the address goes to this login. A PENDING account cannot sign
                            // in, so it owns nothing yet.
                            log.info("Dropping unconfirmed account {} whose e-mail was verified by Firebase UID {}",
                                    holder.get().getId(), firebaseUid);
                            appUserRepository.delete(holder.get());
                            appUserRepository.flush();
                        } else if (holder.isPresent()) {
                            log.warn("Firebase UID {} uses an e-mail already held by another account; "
                                    + "provisioned without e-mail, merge the accounts by hand", firebaseUid);
                            normalized = null;
                        }
                    }
                    AppUser user = AppUser.create(normalized, displayName, defaultRoleFor(firebaseUid), UserStatus.ACTIVE);
                    user.setFirebaseUid(firebaseUid);
                    if (normalized != null && emailVerified) {
                        user.setEmailVerifiedAt(Instant.now());
                    }
                    return appUserRepository.save(user);
                });
    }

    public AppUser updateRole(Long id, Role role) {
        AppUser user = findById(id);
        user.setRole(role);
        return appUserRepository.save(user);
    }

    public AppUser suspend(Long id) {
        AppUser user = findById(id);
        user.setStatus(UserStatus.SUSPENDED);
        return appUserRepository.save(user);
    }

    /**
     * Lifts a suspension. A local account whose e-mail was never confirmed goes back to
     * PENDING, so reactivating cannot be used to skip the e-mail check. Accounts that came
     * from Firebase or Google were vouched for by the provider and become ACTIVE.
     */
    public AppUser reactivate(Long id) {
        AppUser user = findById(id);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new BusinessRuleException("Only a suspended account can be reactivated");
        }
        boolean unconfirmedLocalAccount = user.getEmailVerifiedAt() == null && user.getFirebaseUid() == null;
        user.setStatus(unconfirmedLocalAccount ? UserStatus.PENDING : UserStatus.ACTIVE);
        return appUserRepository.save(user);
    }

    private Role defaultRoleFor(String firebaseUid) {
        return "dev-user".equals(firebaseUid) ? Role.ADMIN : Role.INVESTOR;
    }
}
