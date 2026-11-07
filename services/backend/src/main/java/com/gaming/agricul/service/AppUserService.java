package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
