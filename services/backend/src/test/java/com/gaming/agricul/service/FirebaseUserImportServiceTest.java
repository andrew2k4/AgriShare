package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.repository.AppUserRepository;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FirebaseUserImportServiceTest {

    @Mock
    private FirebaseAuth firebaseAuth;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private FirebaseUserImportService service;

    @Test
    void importsMissingAccounts_syncsExistingOnes_andSkipsAnonymousOnes() throws Exception {
        ExportedUserRecord newUser = record("fb-new", "New@Example.com", true, false, true);
        ExportedUserRecord known = record("fb-known", "known@example.com", true, false, true);
        ExportedUserRecord disabled = record("fb-off", "off@example.com", false, true, true);
        ExportedUserRecord anonymous = record("fb-anon", null, false, false, false);
        ListUsersPage page = mock(ListUsersPage.class);
        when(page.iterateAll()).thenReturn(List.of(newUser, known, disabled, anonymous));
        when(firebaseAuth.listUsers(null)).thenReturn(page);

        AppUser created = AppUser.create("new@example.com", "New", Role.INVESTOR, UserStatus.ACTIVE);
        when(appUserRepository.findByFirebaseUid("fb-new")).thenReturn(Optional.empty());
        when(appUserService.findOrCreateByUid(anyString(), any(), any(), anyBoolean())).thenReturn(created);

        AppUser knownUser = AppUser.create("known@example.com", "Known", Role.INVESTOR, UserStatus.ACTIVE);
        when(appUserRepository.findByFirebaseUid("fb-known")).thenReturn(Optional.of(knownUser));

        AppUser offUser = AppUser.create("off@example.com", "Off", Role.INVESTOR, UserStatus.ACTIVE);
        offUser.setEmailVerifiedAt(Instant.now());
        when(appUserRepository.findByFirebaseUid("fb-off")).thenReturn(Optional.of(offUser));

        FirebaseUserImportService.Report report = service.importAll();

        assertThat(report).isEqualTo(new FirebaseUserImportService.Report(1, 1, 1, 1, 0, 1));
        verify(appUserService).findOrCreateByUid("fb-new", "New@Example.com", "Name", true);
        assertThat(knownUser.isEmailVerified()).as("verified state copied from Firebase").isTrue();
        // Existing account disabled in Firebase: reported, not suspended behind an admin's back
        assertThat(offUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(appUserRepository, never()).findByFirebaseUid("fb-anon");
    }

    @Test
    void secondRun_changesNothing() throws Exception {
        ExportedUserRecord known = record("fb-known", "known@example.com", true, false, true);
        ListUsersPage page = mock(ListUsersPage.class);
        when(page.iterateAll()).thenReturn(List.of(known));
        when(firebaseAuth.listUsers(null)).thenReturn(page);
        AppUser knownUser = AppUser.create("known@example.com", "Known", Role.INVESTOR, UserStatus.ACTIVE);
        knownUser.setEmailVerifiedAt(Instant.now());
        when(appUserRepository.findByFirebaseUid("fb-known")).thenReturn(Optional.of(knownUser));

        assertThat(service.importAll()).isEqualTo(new FirebaseUserImportService.Report(0, 0, 1, 0, 0, 0));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void newAccountDisabledInFirebase_isCreatedSuspended() throws Exception {
        ExportedUserRecord disabled = record("fb-off", "off@example.com", true, true, true);
        ListUsersPage page = mock(ListUsersPage.class);
        when(page.iterateAll()).thenReturn(List.of(disabled));
        when(firebaseAuth.listUsers(null)).thenReturn(page);
        when(appUserRepository.findByFirebaseUid("fb-off")).thenReturn(Optional.empty());
        AppUser created = AppUser.create("off@example.com", "Off", Role.INVESTOR, UserStatus.ACTIVE);
        when(appUserService.findOrCreateByUid(anyString(), any(), any(), anyBoolean())).thenReturn(created);

        service.importAll();

        assertThat(created.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void emailChangedInFirebase_isCopied_andMustBeVerifiedAgain() throws Exception {
        ExportedUserRecord moved = record("fb-moved", "New.Address@example.com", false, false, true);
        ListUsersPage page = mock(ListUsersPage.class);
        when(page.iterateAll()).thenReturn(List.of(moved));
        when(firebaseAuth.listUsers(null)).thenReturn(page);
        AppUser user = AppUser.create("old@example.com", "Moved", Role.INVESTOR, UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        when(appUserRepository.findByFirebaseUid("fb-moved")).thenReturn(Optional.of(user));
        when(appUserRepository.existsByEmail("new.address@example.com")).thenReturn(false);

        assertThat(service.importAll().updated()).isEqualTo(1);
        assertThat(user.getEmail()).isEqualTo("new.address@example.com");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void emailChangedInFirebaseToOneTakenHere_isNotCopied() throws Exception {
        ExportedUserRecord moved = record("fb-moved", "taken@example.com", true, false, true);
        ListUsersPage page = mock(ListUsersPage.class);
        when(page.iterateAll()).thenReturn(List.of(moved));
        when(firebaseAuth.listUsers(null)).thenReturn(page);
        AppUser user = AppUser.create("old@example.com", "Moved", Role.INVESTOR, UserStatus.ACTIVE);
        when(appUserRepository.findByFirebaseUid("fb-moved")).thenReturn(Optional.of(user));
        when(appUserRepository.existsByEmail("taken@example.com")).thenReturn(true);

        service.importAll();

        assertThat(user.getEmail()).isEqualTo("old@example.com");
    }

    private static ExportedUserRecord record(String uid, String email, boolean verified, boolean disabled,
                                             boolean hasProvider) {
        ExportedUserRecord record = mock(ExportedUserRecord.class);
        when(record.getUid()).thenReturn(uid);
        when(record.getEmail()).thenReturn(email);
        when(record.getDisplayName()).thenReturn("Name");
        when(record.isEmailVerified()).thenReturn(verified);
        when(record.isDisabled()).thenReturn(disabled);
        when(record.getProviderData()).thenReturn(hasProvider ? new UserInfo[]{mock(UserInfo.class)} : new UserInfo[0]);
        return record;
    }

}
