package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gaming.agricul.UserStatus;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void findOrCreateByUid_returnsExistingUser_whenAlreadyProvisioned() {
        AppUser existing = existingUser(Role.MANAGER);
        when(appUserRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(existing));

        AppUser result = appUserService.findOrCreateByUid("uid-1", "a@b.com", "Alice", true);

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void findOrCreateByUid_defaultsToInvestorRole_forRegularFirebaseUser() {
        when(appUserRepository.findByFirebaseUid("uid-2")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("uid-2", "b@c.com", "Bob", false);

        assertThat(result.getRole()).isEqualTo(Role.INVESTOR);
        assertThat(result.getFirebaseUid()).isEqualTo("uid-2");
    }

    @Test
    void findOrCreateByUid_defaultsToAdminRole_forDevUserFallback() {
        when(appUserRepository.findByFirebaseUid("dev-user")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("dev-user", null, "Dev User", false);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_changesRoleAndPersists() {
        AppUser existing = existingUser(Role.INVESTOR);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.updateRole(1L, Role.MANAGER);

        assertThat(result.getRole()).isEqualTo(Role.MANAGER);
        verify(appUserRepository).save(existing);
    }

    @Test
    void findById_throwsNotFoundException_whenMissing() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findOrCreateByUid_storesTheEmailLowerCased() {
        when(appUserRepository.findByFirebaseUid("uid-3")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("uid-3", " Carol@Example.COM ", "Carol", true);

        assertThat(result.getEmail()).isEqualTo("carol@example.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    void findOrCreateByUid_doesNotMarkAnUnverifiedProviderEmailAsVerified() {
        when(appUserRepository.findByFirebaseUid("uid-4")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("uid-4", "dan@example.com", "Dan", false);

        assertThat(result.getEmail()).isEqualTo("dan@example.com");
        assertThat(result.isEmailVerified()).isFalse();
    }

    @Test
    void findOrCreateByUid_provisionsWithoutEmail_whenTheEmailBelongsToAnotherAccount() {
        // e.g. a Firebase account deleted and re-created under a new UID
        when(appUserRepository.findByFirebaseUid("uid-new")).thenReturn(Optional.empty());
        when(appUserRepository.existsByEmail("alice@example.com")).thenReturn(true);
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("uid-new", "Alice@example.com", "Alice", true);

        assertThat(result.getEmail()).isNull();
        assertThat(result.isEmailVerified()).isFalse();
        assertThat(result.getFirebaseUid()).isEqualTo("uid-new");
    }

    @Test
    void findOrCreateByUid_acceptsProviderEmailsOurOwnFormRulesWouldReject() {
        when(appUserRepository.findByFirebaseUid("uid-5")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("uid-5", "admin@localhost", "Admin", true);

        assertThat(result.getEmail()).isEqualTo("admin@localhost");
    }

    private static AppUser existingUser(Role role) {
        AppUser user = AppUser.create("a@b.com", "Alice", role, UserStatus.ACTIVE);
        user.setId(1L);
        user.setFirebaseUid("uid-1");
        return user;
    }
}
