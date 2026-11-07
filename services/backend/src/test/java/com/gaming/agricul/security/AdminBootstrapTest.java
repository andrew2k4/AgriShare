package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.repository.AppUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final AdminBootstrap bootstrap = new AdminBootstrap(" Boss@AgriShare.farm ", repository);

    @Test
    void verifiedBootstrapAccount_becomesAdmin() {
        AppUser user = user("boss@agrishare.farm", true);
        bootstrap.apply(user);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(repository).save(user);
    }

    @Test
    void onceAnAdminExists_nothingIsPromoted_soADemotionSticks() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);
        AppUser demoted = user("boss@agrishare.farm", true);

        bootstrap.apply(demoted);

        assertThat(demoted.getRole()).isEqualTo(Role.INVESTOR);
        verify(repository, never()).save(any());
    }

    @Test
    void unverifiedAddress_isNotPromoted() {
        // Someone who registered the address but could not confirm it
        AppUser user = user("boss@agrishare.farm", false);
        bootstrap.apply(user);
        assertThat(user.getRole()).isEqualTo(Role.INVESTOR);
    }

    @Test
    void otherAccounts_andAnEmptySetting_changeNothing() {
        AppUser other = user("someone@agrishare.farm", true);
        bootstrap.apply(other);
        assertThat(other.getRole()).isEqualTo(Role.INVESTOR);

        AppUser boss = user("boss@agrishare.farm", true);
        new AdminBootstrap("", repository).apply(boss);
        assertThat(boss.getRole()).isEqualTo(Role.INVESTOR);
    }

    private static AppUser user(String email, boolean verified) {
        AppUser user = AppUser.create(email, "U", Role.INVESTOR, UserStatus.ACTIVE);
        if (verified) {
            user.setEmailVerifiedAt(Instant.now());
        }
        return user;
    }
}
