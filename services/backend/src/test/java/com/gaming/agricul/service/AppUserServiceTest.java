package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gaming.agricul.UserStatus;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserService appUserService;




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
    void reactivate_sendsAnUnconfirmedLocalAccountBackToPending() {
        AppUser user = AppUser.create("p@example.com", "P", Role.INVESTOR, UserStatus.SUSPENDED);
        user.setId(5L);
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(appUserService.reactivate(5L).getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void reactivate_restoresAConfirmedAccount() {
        AppUser user = AppUser.create("a@example.com", "A", Role.INVESTOR, UserStatus.SUSPENDED);
        user.setId(6L);
        user.setEmailVerifiedAt(Instant.now());
        when(appUserRepository.findById(6L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(appUserService.reactivate(6L).getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void reactivate_refusesAnAccountThatIsNotSuspended() {
        AppUser user = AppUser.create("p@example.com", "P", Role.INVESTOR, UserStatus.PENDING);
        user.setId(7L);
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.reactivate(7L)).isInstanceOf(BusinessRuleException.class);
    }



    private static AppUser existingUser(Role role) {
        AppUser user = AppUser.create("a@b.com", "Alice", role, UserStatus.ACTIVE);
        user.setId(1L);
        user.setFirebaseUid("uid-1");
        return user;
    }

}
