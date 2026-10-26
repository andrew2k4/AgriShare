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

import java.time.Instant;
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
        AppUser existing = new AppUser(1L, "uid-1", "a@b.com", "Alice", Role.MANAGER, Instant.now());
        when(appUserRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(existing));

        AppUser result = appUserService.findOrCreateByUid("uid-1", "a@b.com", "Alice");

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void findOrCreateByUid_defaultsToInvestorRole_forRegularFirebaseUser() {
        when(appUserRepository.findByFirebaseUid("uid-2")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("uid-2", "b@c.com", "Bob");

        assertThat(result.getRole()).isEqualTo(Role.INVESTOR);
        assertThat(result.getFirebaseUid()).isEqualTo("uid-2");
    }

    @Test
    void findOrCreateByUid_defaultsToAdminRole_forDevUserFallback() {
        when(appUserRepository.findByFirebaseUid("dev-user")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.findOrCreateByUid("dev-user", null, "Dev User");

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_changesRoleAndPersists() {
        AppUser existing = new AppUser(1L, "uid-1", "a@b.com", "Alice", Role.INVESTOR, Instant.now());
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
}
