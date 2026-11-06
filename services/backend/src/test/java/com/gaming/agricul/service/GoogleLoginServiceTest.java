package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.IdentityProvider;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserIdentity;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.repository.AppUserRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.UserIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleLoginServiceTest {

    private static final String SUB = "google-sub-123";

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserIdentityRepository userIdentityRepository;
    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private GoogleLoginService service;

    @BeforeEach
    void saveReturnsItsArgument() {
        when(appUserRepository.save(any())).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(100L);
            }
            return user;
        });
        when(userIdentityRepository.findByProviderAndSubject(any(), any())).thenReturn(Optional.empty());
        when(appUserRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userIdentityRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    void alreadyLinkedIdentity_signsInTheLinkedAccount_whateverTheEmail() {
        AppUser user = user(7L, "old@example.com", UserStatus.ACTIVE, true);
        when(userIdentityRepository.findByProviderAndSubject(IdentityProvider.GOOGLE, SUB)).thenReturn(Optional.of(identity(7L)));
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));

        GoogleLoginService.Result login = service.login(SUB, "new@example.com", true, "Marie");
        AppUser result = login.user();

        assertThat(result).isSameAs(user);
        assertThat(login.revokeExistingSessions()).isFalse();
        assertThat(result.getEmail()).isEqualTo("old@example.com");
        assertThat(result.getLastLoginAt()).isNotNull();
        verify(userIdentityRepository, never()).save(any());
    }

    @Test
    void linkedButSuspended_isRefused() {
        when(userIdentityRepository.findByProviderAndSubject(IdentityProvider.GOOGLE, SUB)).thenReturn(Optional.of(identity(7L)));
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user(7L, "a@example.com", UserStatus.SUSPENDED, true)));

        assertCode(() -> service.login(SUB, "a@example.com", true, "A"), "ACCOUNT_SUSPENDED");
    }

    @Test
    void unverifiedGoogleEmail_isRefused_beforeLookingForAnAccount() {
        assertCode(() -> service.login(SUB, "a@example.com", false, "A"), "GOOGLE_EMAIL_NOT_VERIFIED");
        verify(appUserRepository, never()).findByEmail(any());
    }

    @Test
    void unknownEmail_createsAnActiveVerifiedInvestor_andLinksIt() {
        GoogleLoginService.Result login = service.login(SUB, "New@Example.com", true, "Nouvel Utilisateur");
        AppUser result = login.user();

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getRole()).isEqualTo(Role.INVESTOR);
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(result.hasPassword()).isFalse();
        assertThat(result.getDisplayName()).isEqualTo("Nouvel Utilisateur");
        assertLinked(100L);
    }

    @Test
    void verifiedLocalAccount_isLinked_andKeepsItsPassword() {
        AppUser local = user(8L, "marie@example.com", UserStatus.ACTIVE, true);
        local.setPasswordHash("{argon2}hash");
        local.setUsername("marie");
        when(appUserRepository.findByEmail("marie@example.com")).thenReturn(Optional.of(local));

        GoogleLoginService.Result login = service.login(SUB, "marie@example.com", true, "Marie");
        AppUser result = login.user();

        assertThat(result.getPasswordHash()).isEqualTo("{argon2}hash");
        assertThat(result.getUsername()).isEqualTo("marie");
        assertThat(login.revokeExistingSessions()).isFalse();
        assertLinked(8L);
    }

    @Test
    void unconfirmedRegistration_goesToTheGoogleUser() {
        AppUser squatter = user(9L, "victim@example.com", UserStatus.PENDING, false);
        squatter.setPasswordHash("{argon2}strangers");
        squatter.setUsername("stranger");
        squatter.setDisplayName("Stranger");
        when(appUserRepository.findByEmail("victim@example.com")).thenReturn(Optional.of(squatter));

        GoogleLoginService.Result login = service.login(SUB, "victim@example.com", true, "Vraie Victime");
        AppUser result = login.user();

        assertThat(result.hasPassword()).isFalse();
        assertThat(result.getUsername()).isNull();
        assertThat(result.getDisplayName()).isEqualTo("Vraie Victime");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(login.revokeExistingSessions()).isTrue();
        assertLinked(9L);
    }

    @Test
    void unverifiedFirebaseAccount_goesToTheGoogleUser_andItsFirebaseLoginIsDetached() {
        AppUser firebase = user(10L, "owner@example.com", UserStatus.ACTIVE, false);
        firebase.setFirebaseUid("fb-10");
        firebase.setUsername("owner");
        when(appUserRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(firebase));

        GoogleLoginService.Result login = service.login(SUB, "owner@example.com", true, "Owner");
        AppUser result = login.user();

        assertThat(result.isEmailVerified()).isTrue();
        // Whoever created the Firebase account can no longer reach this one with a Firebase token
        assertThat(result.getFirebaseUid()).isNull();
        // ...and the account keeps its projects, now under its local key
        verify(projectRepository).reassignOwner("fb-10", "app-user:10");
        assertThat(login.revokeExistingSessions()).isTrue();
    }

    @Test
    void accountAlreadyLinkedToAnotherGoogleAccount_isNotLinkedAgainByEmail() {
        // e.g. a Workspace address reassigned to a new person, with a new Google "sub"
        AppUser alice = user(12L, "alice@corp.com", UserStatus.ACTIVE, true);
        UserIdentity previous = identity(12L);
        previous.setSubject("other-sub");
        when(appUserRepository.findByEmail("alice@corp.com")).thenReturn(Optional.of(alice));
        when(userIdentityRepository.findByUserId(12L)).thenReturn(List.of(previous));

        assertCode(() -> service.login(SUB, "alice@corp.com", true, "Nouvelle Alice"), "GOOGLE_ACCOUNT_MISMATCH");
        verify(userIdentityRepository, never()).save(any());
    }

    @Test
    void linkedAccountStillPending_becomesActive_sinceGoogleProvedTheAddress() {
        AppUser pending = user(13L, "p@example.com", UserStatus.PENDING, false);
        when(userIdentityRepository.findByProviderAndSubject(IdentityProvider.GOOGLE, SUB)).thenReturn(Optional.of(identity(13L)));
        when(appUserRepository.findById(13L)).thenReturn(Optional.of(pending));

        AppUser result = service.login(SUB, "p@example.com", true, "P").user();

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.isEmailVerified()).isTrue();
    }

    @Test
    void suspendedAccountWithThatEmail_isRefused_andNotLinked() {
        when(appUserRepository.findByEmail("s@example.com"))
                .thenReturn(Optional.of(user(11L, "s@example.com", UserStatus.SUSPENDED, true)));

        assertCode(() -> service.login(SUB, "s@example.com", true, "S"), "ACCOUNT_SUSPENDED");
        verify(userIdentityRepository, never()).save(any());
    }

    private void assertLinked(Long userId) {
        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getProvider()).isEqualTo(IdentityProvider.GOOGLE);
        assertThat(saved.getValue().getSubject()).isEqualTo(SUB);
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(AuthException.class)
                .satisfies(e -> assertThat(((AuthException) e).getCode()).isEqualTo(code));
    }

    private static AppUser user(Long id, String email, UserStatus status, boolean verified) {
        AppUser user = AppUser.create(email, "Name", Role.INVESTOR, status);
        user.setId(id);
        if (verified) {
            user.setEmailVerifiedAt(Instant.now());
        }
        return user;
    }

    private static UserIdentity identity(Long userId) {
        UserIdentity identity = new UserIdentity();
        identity.setUserId(userId);
        identity.setProvider(IdentityProvider.GOOGLE);
        identity.setSubject(SUB);
        return identity;
    }
}
