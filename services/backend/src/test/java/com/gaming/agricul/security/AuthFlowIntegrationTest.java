package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.mail.AccountAlreadyExistsNotice;
import com.gaming.agricul.mail.EmailVerificationRequested;
import com.gaming.agricul.mail.PasswordResetRequested;
import com.gaming.agricul.repository.AppUserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of local accounts and sessions through the real security chain (the
 * secured one: firebase.enabled=true, with Firebase itself mocked out), real cookies, CSRF
 * and Spring Session on H2.
 */
// The service account is never read: FirebaseApp and FirebaseAuth are mocked below
@SpringBootTest(properties = {"firebase.enabled=true", "firebase.service-account-path=classpath:unused.json"})
@AutoConfigureMockMvc
@RecordApplicationEvents
class AuthFlowIntegrationTest {

    private static final String SESSION_COOKIE = "__Host-SESSION";
    private static final String CSRF_COOKIE = "__Host-XSRF-TOKEN";
    private static final String PASSWORD = "correct-horse";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private AppUserRepository users;

    @MockitoBean
    private FirebaseApp firebaseApp;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    /** Firebase's password endpoint, mocked; by default no account is migratable. */
    @MockitoBean
    private FirebasePasswordCheck firebasePasswordCheck;

    /** Unique per test: rate-limit counters are keyed by IP and must not leak between tests. */
    private String clientIp;
    private String email;
    private String username;

    @BeforeEach
    void uniqueIdentity() {
        int n = SEQUENCE.incrementAndGet();
        clientIp = "203.0.113." + n;
        email = "user" + n + "@example.com";
        username = "user" + n;
    }

    // ---------------------------------------------------------------- the happy path

    @Test
    void registerVerifyLoginMeLogout() throws Exception {
        register(email, username, PASSWORD).andExpect(status().isAccepted());

        // Cannot sign in before confirming the address
        login(email, PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        postJson("/api/auth/verify-email", "{\"token\":\"" + verificationToken(email) + "\"}")
                .andExpect(status().isOk());

        MvcResult loggedIn = login(username.toUpperCase(), PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("INVESTOR"))
                .andReturn();
        Cookie session = sessionCookie(loggedIn);
        assertThat(session.isHttpOnly()).isTrue();
        assertThat(session.getSecure()).isTrue();

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.emailVerified").value(true));

        postJson("/api/auth/logout", "{}", session).andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void verificationLinkWorksOnlyOnce() throws Exception {
        register(email, username, PASSWORD).andExpect(status().isAccepted());
        String body = "{\"token\":\"" + verificationToken(email) + "\"}";

        postJson("/api/auth/verify-email", body).andExpect(status().isOk());
        postJson("/api/auth/verify-email", body).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- CSRF and 401/403

    @Test
    void stateChangingRequestWithoutCsrfToken_isRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"x\",\"password\":\"y\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF"));
    }

    @Test
    void csrfTokenFromAnotherCookie_isRejected() throws Exception {
        Csrf csrf = csrf();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"x\",\"password\":\"y\"}")
                        .cookie(new Cookie(CSRF_COOKIE, "forged"))
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF"));
    }

    @Test
    void protectedEndpointWithoutSession_is401() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- enumeration and brute force

    @Test
    void wrongPasswordAndUnknownAccount_failTheSameWay() throws Exception {
        createActiveUser(email, username, PASSWORD);

        String wrongPassword = login(email, "not-the-password").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownUser = login("nobody-" + email, "not-the-password").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(withoutTimestamp(wrongPassword)).isEqualTo(withoutTimestamp(unknownUser));
        assertThat(JsonPath.<String>read(wrongPassword, "$.code")).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void fiveWrongPasswords_lockTheAccount_evenForTheRightPassword() throws Exception {
        createActiveUser(email, username, PASSWORD);
        for (int i = 0; i < AuthServiceLimits.MAX_FAILED_LOGINS; i++) {
            login(email, "wrong-" + i).andExpect(status().isUnauthorized());
        }

        login(email, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"))
                .andExpect(header().exists("Retry-After"));

        AppUser user = users.findByEmail(email).orElseThrow();
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void registeringATakenEmail_looksLikeSuccess_andWarnsTheOwner() throws Exception {
        createActiveUser(email, username, PASSWORD);

        register(email, "other" + username, "another-password").andExpect(status().isAccepted());

        assertThat(events.stream(AccountAlreadyExistsNotice.class).map(AccountAlreadyExistsNotice::email))
                .containsExactly(email);
        // Only the verification e-mail of the original registration, none for the second one
        assertThat(events.stream(EmailVerificationRequested.class)).hasSize(1);
        // The existing account is untouched
        login("other" + username, "another-password").andExpect(status().isUnauthorized());
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void takenUsername_isReported() throws Exception {
        createActiveUser(email, username, PASSWORD);

        register("other-" + email, username, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void unconfirmedRegistration_canBeTakenOverByTheRealOwner() throws Exception {
        // A stranger registers someone else's address but can never confirm it
        register(email, username, "strangers-password").andExpect(status().isAccepted());
        // The real owner registers too, and confirms with the link they receive
        register(email, "owner" + username, PASSWORD).andExpect(status().isAccepted());
        List<String> tokens = events.stream(EmailVerificationRequested.class)
                .filter(e -> e.email().equals(email)).map(EmailVerificationRequested::rawToken).toList();
        assertThat(tokens).hasSize(2);

        // The stranger's link was voided when the new one was issued
        postJson("/api/auth/verify-email", "{\"token\":\"" + tokens.get(0) + "\"}").andExpect(status().isBadRequest());
        postJson("/api/auth/verify-email", "{\"token\":\"" + tokens.get(1) + "\"}").andExpect(status().isOk());

        login(email, "strangers-password").andExpect(status().isUnauthorized());
        login("owner" + username, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void passwordTooShort_isA400() throws Exception {
        register(email, username, "1234567").andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- sessions

    @Test
    void sessionIdChangesAtLogin() throws Exception {
        createActiveUser(email, username, PASSWORD);
        Cookie first = sessionCookie(login(email, PASSWORD).andExpect(status().isOk()).andReturn());

        MvcResult second = mvc.perform(withCsrf(post("/api/auth/login"), csrf())
                        .content(loginJson(email, PASSWORD)).cookie(first))
                .andExpect(status().isOk()).andReturn();

        assertThat(sessionCookie(second).getValue()).isNotEqualTo(first.getValue());
        // The pre-login session no longer works
        mvc.perform(get("/api/auth/me").cookie(first)).andExpect(status().isUnauthorized());
    }

    @Test
    void sessionsCanBeListedAndRevokedOneByOne_orAllAtOnce() throws Exception {
        createActiveUser(email, username, PASSWORD);
        Cookie laptop = sessionCookie(login(email, PASSWORD).andReturn());
        Cookie phone = sessionCookie(login(email, PASSWORD).andReturn());
        Cookie tablet = sessionCookie(login(email, PASSWORD).andReturn());

        String list = mvc.perform(get("/api/auth/sessions").cookie(laptop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.current == true)].ipAddress").value(clientIp))
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain(laptop.getValue());

        // The phone's handle, as the phone itself sees it, revoked from the laptop
        List<String> phoneHandles = JsonPath.read(mvc.perform(get("/api/auth/sessions").cookie(phone))
                .andReturn().getResponse().getContentAsString(), "$[?(@.current == true)].handle");
        Csrf csrf = csrf();
        mvc.perform(delete("/api/auth/sessions/" + phoneHandles.get(0)).cookie(laptop, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").cookie(phone)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(tablet)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(laptop)).andExpect(status().isOk());

        postJson("/api/auth/logout-all", "{}", laptop).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(laptop)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(tablet)).andExpect(status().isUnauthorized());
    }

    @Test
    void anotherUsersSessionHandle_cannotBeRevoked() throws Exception {
        createActiveUser(email, username, PASSWORD);
        createActiveUser("b-" + email, "b" + username, PASSWORD);
        Cookie mine = sessionCookie(login(email, PASSWORD).andReturn());
        Cookie theirs = sessionCookie(login("b-" + email, PASSWORD).andReturn());
        String theirHandle = JsonPath.read(mvc.perform(get("/api/auth/sessions").cookie(theirs))
                .andReturn().getResponse().getContentAsString(), "$[0].handle");

        Csrf csrf = csrf();
        mvc.perform(delete("/api/auth/sessions/" + theirHandle).cookie(mine, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/me").cookie(theirs)).andExpect(status().isOk());
    }

    @Test
    void suspension_endsExistingSessions_andBlocksLogin() throws Exception {
        createActiveUser(email, username, PASSWORD);
        Cookie session = sessionCookie(login(email, PASSWORD).andReturn());

        AppUser user = users.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        users.save(user);

        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
        login(email, PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    void roleChange_appliesToAnOpenSessionImmediately() throws Exception {
        createActiveUser(email, username, PASSWORD);
        Cookie session = sessionCookie(login(email, PASSWORD).andReturn());
        mvc.perform(get("/api/users").cookie(session)).andExpect(status().isConflict()); // not admin

        AppUser user = users.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        users.save(user);

        mvc.perform(get("/api/users").cookie(session)).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- passwords

    @Test
    void forgotThenReset_changesThePassword_andEndsEverySession() throws Exception {
        createActiveUser(email, username, PASSWORD);
        Cookie oldSession = sessionCookie(login(email, PASSWORD).andReturn());

        postJson("/api/auth/password/forgot", "{\"email\":\"" + email + "\"}").andExpect(status().isAccepted());
        String token = events.stream(PasswordResetRequested.class)
                .filter(e -> e.email().equals(email)).map(PasswordResetRequested::rawToken).findFirst().orElseThrow();

        postJson("/api/auth/password/reset", "{\"token\":\"" + token + "\",\"newPassword\":\"brand-new-pass\"}")
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").cookie(oldSession)).andExpect(status().isUnauthorized());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "brand-new-pass").andExpect(status().isOk());
    }

    @Test
    void forgotPasswordForAnUnknownAddress_looksTheSame() throws Exception {
        postJson("/api/auth/password/forgot", "{\"email\":\"nobody-" + email + "\"}")
                .andExpect(status().isAccepted());
        assertThat(events.stream(PasswordResetRequested.class)).isEmpty();
    }

    @Test
    void changePassword_needsTheCurrentOne_andEndsOtherSessionsOnly() throws Exception {
        createActiveUser(email, username, PASSWORD);
        Cookie current = sessionCookie(login(email, PASSWORD).andReturn());
        Cookie other = sessionCookie(login(email, PASSWORD).andReturn());

        postJson("/api/auth/password/change",
                "{\"currentPassword\":\"wrong\",\"newPassword\":\"brand-new-pass\"}", current)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));

        postJson("/api/auth/password/change",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"brand-new-pass\"}", current)
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").cookie(current)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(other)).andExpect(status().isUnauthorized());
        login(email, "brand-new-pass").andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- review regressions

    @Test
    void passwordUsers_seeOnlyTheirOwnProjects() throws Exception {
        createActiveUser(email, username, PASSWORD);
        createActiveUser("b-" + email, "b" + username, PASSWORD);
        Cookie alice = sessionCookie(login(email, PASSWORD).andReturn());
        Cookie bob = sessionCookie(login("b-" + email, PASSWORD).andReturn());

        postJson("/api/projects", "{\"name\":\"Champ d'Alice\",\"projectType\":\"AGRICULTURE\"}", alice)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUid").value("app-user:" + users.findByEmail(email).orElseThrow().getId()));

        mvc.perform(get("/api/projects").cookie(alice)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/projects").cookie(bob)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void lockedAccount_isOnlyReportedToWhoeverKnowsThePassword() throws Exception {
        createActiveUser(email, username, PASSWORD);
        AppUser user = users.findByEmail(email).orElseThrow();
        // As after a restart: locked in the database, no in-memory counter
        user.setLockedUntil(Instant.now().plusSeconds(600));
        users.save(user);

        login(email, "wrong-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(username, PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void oversizedCredentials_areRefusedBeforeHashing() throws Exception {
        createActiveUser(email, username, PASSWORD);
        String huge = "x".repeat(10_000);

        login(email, huge).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(huge + "@example.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void loginClearsTheCsrfCookie_soANewTokenIsFetchedAfterwards() throws Exception {
        createActiveUser(email, username, PASSWORD);

        MvcResult result = login(email, PASSWORD).andExpect(status().isOk()).andReturn();

        Cookie cleared = result.getResponse().getCookie(CSRF_COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
    }

    @Test
    void retryAfter_isReadableByTheFrontend() throws Exception {
        mvc.perform(get("/api/auth/csrf").header("Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Expose-Headers", org.hamcrest.Matchers.containsString("Retry-After")));
    }

    // ---------------------------------------------------------------- Firebase migration

    @Test
    void firebaseAccount_migratesItsPasswordAtFirstLogin_thenNeverAsksFirebaseAgain() throws Exception {
        AppUser firebase = firebaseAccount(email, "fb-" + username);
        org.mockito.Mockito.when(firebasePasswordCheck.canMigrate(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> !((AppUser) invocation.getArgument(0)).hasPassword());
        org.mockito.Mockito.when(firebasePasswordCheck.check(email, PASSWORD, "fb-" + username))
                .thenReturn(FirebasePasswordCheck.Outcome.MATCH);

        login(email, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.hasPassword").value(true));
        assertThat(users.findById(firebase.getId()).orElseThrow().getPasswordHash()).startsWith("{argon2}");

        login(email, PASSWORD).andExpect(status().isOk());
        org.mockito.Mockito.verify(firebasePasswordCheck, org.mockito.Mockito.times(1))
                .check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void firebaseAccount_wrongPassword_countsTowardsTheLock() throws Exception {
        AppUser firebase = firebaseAccount(email, "fb-" + username);
        org.mockito.Mockito.when(firebasePasswordCheck.canMigrate(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        org.mockito.Mockito.when(firebasePasswordCheck.check(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(FirebasePasswordCheck.Outcome.NO_MATCH);

        login(email, "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        assertThat(users.findById(firebase.getId()).orElseThrow().getFailedLoginCount()).isEqualTo(1);
        assertThat(users.findById(firebase.getId()).orElseThrow().hasPassword()).isFalse();
    }

    @Test
    void firebaseDown_isA503_notAWrongPassword() throws Exception {
        firebaseAccount(email, "fb-" + username);
        org.mockito.Mockito.when(firebasePasswordCheck.canMigrate(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        org.mockito.Mockito.when(firebasePasswordCheck.check(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(FirebasePasswordCheck.Outcome.UNAVAILABLE);

        login(email, PASSWORD).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("LOGIN_UNAVAILABLE"));
    }

    @Test
    void suspendedFirebaseAccount_isNotCheckedWithFirebase_norCountedAsAFailure() throws Exception {
        AppUser firebase = firebaseAccount(email, "fb-" + username);
        firebase.setStatus(UserStatus.SUSPENDED);
        users.save(firebase);
        org.mockito.Mockito.when(firebasePasswordCheck.canMigrate(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        login(email, PASSWORD).andExpect(status().isUnauthorized());

        org.mockito.Mockito.verify(firebasePasswordCheck, org.mockito.Mockito.never())
                .check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(users.findById(firebase.getId()).orElseThrow().getFailedLoginCount()).isZero();
    }

    @Test
    void firebaseUnavailable_stillCountsOnTheIdentifier() throws Exception {
        firebaseAccount(email, "fb-" + username);
        org.mockito.Mockito.when(firebasePasswordCheck.canMigrate(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        org.mockito.Mockito.when(firebasePasswordCheck.check(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(FirebasePasswordCheck.Outcome.UNAVAILABLE);

        for (int i = 0; i < 5; i++) {
            login(email, PASSWORD).andExpect(status().isServiceUnavailable());
        }
        login(email, PASSWORD).andExpect(status().isTooManyRequests());
        org.mockito.Mockito.verify(firebasePasswordCheck, org.mockito.Mockito.times(5))
                .check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void accountWithoutUsername_choosesOneOnce() throws Exception {
        AppUser firebase = firebaseAccount(email, "fb-" + username);
        firebase.setPasswordHash(new org.springframework.security.crypto.argon2.Argon2PasswordEncoder(16, 32, 1, 1 << 12, 1)
                .encode(PASSWORD).replaceFirst("^", "{argon2}"));
        users.save(firebase);
        Cookie session = sessionCookie(login(email, PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").doesNotExist()).andReturn());

        Csrf csrf = csrf();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/username")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + username + "\"}")
                        .cookie(session, csrf.cookie()).header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        Csrf again = csrf();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/username")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"other" + username + "\"}")
                        .cookie(session, again.cookie()).header("X-XSRF-TOKEN", again.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_SET"));

        login(username, PASSWORD).andExpect(status().isOk());
    }

    /** An account as Firebase provisioning leaves it: active, Firebase UID, no local password. */
    private AppUser firebaseAccount(String email, String uid) {
        AppUser user = AppUser.create(email, "Firebase User", Role.INVESTOR, UserStatus.ACTIVE);
        user.setFirebaseUid(uid);
        user.setEmailVerifiedAt(Instant.now());
        return users.save(user);
    }

    // ---------------------------------------------------------------- helpers

    private record Csrf(Cookie cookie, String token) {}

    private Csrf lastCsrf;

    private Csrf csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie(CSRF_COOKIE);
        assertThat(cookie).as("CSRF cookie").isNotNull();
        lastCsrf = new Csrf(cookie, JsonPath.read(result.getResponse().getContentAsString(), "$.token"));
        return lastCsrf;
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, Csrf csrf) {
        return request.contentType(MediaType.APPLICATION_JSON)
                // What Tomcat's RemoteIpValve leaves after reading X-Real-IP from Nginx
                .with(request1 -> {
                    request1.setRemoteAddr(clientIp);
                    return request1;
                })
                .cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token());
    }

    private ResultActions postJson(String url, String json, Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder request = withCsrf(post(url), csrf()).content(json);
        List<Cookie> all = new ArrayList<>(List.of(cookies));
        if (!all.isEmpty()) {
            all.add(lastCsrf.cookie());
            request.cookie(all.toArray(Cookie[]::new));
        }
        return mvc.perform(request);
    }

    private ResultActions register(String email, String username, String password) throws Exception {
        return postJson("/api/auth/register", "{\"email\":\"" + email + "\",\"username\":\"" + username
                + "\",\"password\":\"" + password + "\"}");
    }

    private ResultActions login(String identifier, String password) throws Exception {
        return postJson("/api/auth/login", loginJson(identifier, password));
    }

    private static String loginJson(String identifier, String password) {
        return "{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}";
    }

    private void createActiveUser(String email, String username, String password) throws Exception {
        register(email, username, password).andExpect(status().isAccepted());
        postJson("/api/auth/verify-email", "{\"token\":\"" + verificationToken(email) + "\"}")
                .andExpect(status().isOk());
    }

    private String verificationToken(String email) {
        return events.stream(EmailVerificationRequested.class)
                .filter(e -> e.email().equals(email))
                .reduce((first, second) -> second)
                .map(EmailVerificationRequested::rawToken)
                .orElseThrow(() -> new AssertionError("no verification e-mail for " + email));
    }

    private static Cookie sessionCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).as("session cookie").isNotNull();
        return cookie;
    }

    private static String withoutTimestamp(String json) {
        return json.replaceAll("\"timestamp\":\"[^\"]*\"", "");
    }

    /** Mirrors AuthService's constant without widening its visibility. */
    private static final class AuthServiceLimits {
        static final int MAX_FAILED_LOGINS = 5;
    }
}
