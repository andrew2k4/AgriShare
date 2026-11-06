package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.security.CredentialRules;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
                    if (normalized != null && appUserRepository.existsByEmail(normalized)) {
                        log.warn("Firebase UID {} uses an e-mail already held by another account; "
                                + "provisioned without e-mail, merge the accounts by hand", firebaseUid);
                        normalized = null;
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

    private Role defaultRoleFor(String firebaseUid) {
        return "dev-user".equals(firebaseUid) ? Role.ADMIN : Role.INVESTOR;
    }
}
