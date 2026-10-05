package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
     */
    public AppUser findOrCreateByUid(String firebaseUid, String email, String displayName) {
        return appUserRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> appUserRepository.save(new AppUser(
                        null, firebaseUid, email, displayName, defaultRoleFor(firebaseUid), Instant.now()
                )));
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
