package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * How the first administrator gets in: while no ADMIN exists, the account with the e-mail
 * BOOTSTRAP_ADMIN_EMAIL is made ADMIN when it signs in, once its address is verified
 * (e-mailed link or Google), so nobody can claim the address by registering it first.
 *
 * <p>As soon as one ADMIN exists it does nothing: an admin can later change that account's
 * role and it stays changed. Called from {@link SessionLogin}, so every way of signing in
 * goes through it.
 */
@Slf4j
@Component
public class AdminBootstrap {

    private final String email;
    private final AppUserRepository appUserRepository;

    public AdminBootstrap(@Value("${app.bootstrap-admin-email:}") String email, AppUserRepository appUserRepository) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.appUserRepository = appUserRepository;
    }

    /** Promotes and saves the user when it is the bootstrap admin and there is no admin yet. */
    public void apply(AppUser user) {
        if (email.isEmpty() || !user.isEmailVerified() || !email.equals(user.getEmail())
                || user.getRole() == Role.ADMIN || appUserRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        user.setRole(Role.ADMIN);
        appUserRepository.save(user);
        log.warn("Account {} made the first ADMIN (BOOTSTRAP_ADMIN_EMAIL)", user.getId());
    }
}
