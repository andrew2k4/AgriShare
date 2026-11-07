package com.gaming.agricul.service;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.gaming.agricul.UserTokenType;
import com.gaming.agricul.exception.AuthException;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.InvalidInputException;
import com.gaming.agricul.mail.AccountAlreadyExistsNotice;
import com.gaming.agricul.mail.EmailVerificationRequested;
import com.gaming.agricul.mail.PasswordResetRequested;
import com.gaming.agricul.repository.AppUserRepository;
import com.gaming.agricul.security.CredentialRules;
import com.gaming.agricul.security.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Local accounts: registration, e-mail verification, password login, password reset/change.
 *
 * <p>Account enumeration: login fails with the same error and in about the same time whether
 * the account exists or not (a dummy hash is checked when there is no real one), and
 * register / forgot-password / resend-verification always answer the same way. Only a taken
 * username is reported, since usernames are meant to be visible.
 *
 * <p>Brute force: per-IP and per-identifier limits in memory (applied identically to unknown
 * identifiers) plus a 15-minute lock stored on the account after 5 wrong passwords, which
 * survives a restart.
 */
@Slf4j
@Service
@Transactional
public class AuthService {

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int LOGINS_PER_IP = 30;
    private static final Duration MAIL_WINDOW = Duration.ofHours(1);
    private static final int MAIL_REQUESTS_PER_IP = 10;
    private static final int MAIL_REQUESTS_PER_ADDRESS = 3;
    private static final int PASSWORD_CHANGES_PER_USER = 5;
    /** Longest possible e-mail; usernames are shorter. */
    private static final int MAX_IDENTIFIER_LENGTH = 254;

    private final AppUserRepository appUserRepository;
    private final UserTokenService userTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final ApplicationEventPublisher events;
    /** Checked when the account does not exist, so that case takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(AppUserRepository appUserRepository, UserTokenService userTokenService,
                       PasswordEncoder passwordEncoder, RateLimiter rateLimiter, ApplicationEventPublisher events) {
        this.appUserRepository = appUserRepository;
        this.userTokenService = userTokenService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.events = events;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // ---------------------------------------------------------------- registration

    public void register(String rawEmail, String rawUsername, String password, String rawDisplayName, String clientIp) {
        String email = CredentialRules.normalizeEmail(rawEmail);
        String username = CredentialRules.normalizeUsername(rawUsername);
        CredentialRules.checkPassword(password);
        String displayName = CredentialRules.cleanDisplayName(rawDisplayName, username);
        limitMailRequest("register", clientIp, email);

        Optional<AppUser> existing = appUserRepository.findByEmail(email);
        if (existing.isPresent() && existing.get().getStatus() != UserStatus.PENDING) {
            events.publishEvent(new AccountAlreadyExistsNotice(email));
            return;
        }

        appUserRepository.findByUsername(username)
                .filter(owner -> existing.isEmpty() || !owner.getId().equals(existing.get().getId()))
                .ifPresent(owner -> {
                    throw new AuthException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "This username is already taken");
                });

        // A PENDING account never proved it owns the address. Registering again replaces its
        // details, so a stranger cannot reserve someone else's e-mail by registering it first.
        AppUser user = existing.orElseGet(() -> AppUser.create(email, displayName, Role.INVESTOR, UserStatus.PENDING));
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user = appUserRepository.save(user);

        String token = userTokenService.issue(user.getId(), UserTokenType.EMAIL_VERIFICATION);
        events.publishEvent(new EmailVerificationRequested(email, token));
    }

    public void verifyEmail(String rawToken) {
        Long userId = userTokenService.consume(rawToken, UserTokenType.EMAIL_VERIFICATION);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidInputException(UserTokenService.INVALID_LINK));
        markEmailVerified(user);
        appUserRepository.save(user);
    }

    public void resendVerification(String rawEmail, String clientIp) {
        String email = CredentialRules.normalizeEmail(rawEmail);
        limitMailRequest("resend", clientIp, email);
        appUserRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.PENDING)
                .ifPresent(user -> {
                    String token = userTokenService.issue(user.getId(), UserTokenType.EMAIL_VERIFICATION);
                    events.publishEvent(new EmailVerificationRequested(email, token));
                });
    }

    // ---------------------------------------------------------------- login

    /**
     * Checks the credentials and returns the signed-in user. The failure counter is written
     * even though the method then throws, hence noRollbackFor.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public AppUser authenticate(String rawIdentifier, String password, String clientIp) {
        String ipKey = "login-ip:" + clientIp;
        if (!rateLimiter.tryAcquire(ipKey, LOGINS_PER_IP, LOGIN_WINDOW)) {
            throw AuthException.tooManyAttempts(rateLimiter.secondsUntilReset(ipKey));
        }
        String identifier = CredentialRules.normalizeIdentifier(rawIdentifier);
        if (identifier.length() > MAX_IDENTIFIER_LENGTH || tooLong(password)) {
            // Never a valid account or password; refused before hashing, so a huge input
            // cannot cost CPU (Argon2) or memory (rate-limit keys)
            throw AuthException.invalidCredentials();
        }
        String identifierKey = "login-id:" + identifier;
        if (rateLimiter.isExhausted(identifierKey, MAX_FAILED_LOGINS)) {
            throw AuthException.tooManyAttempts(rateLimiter.secondsUntilReset(identifierKey));
        }

        Instant now = Instant.now();
        AppUser user = identifier.isEmpty() ? null : (CredentialRules.isEmail(identifier)
                ? appUserRepository.findByEmail(identifier)
                : appUserRepository.findByUsername(identifier)).orElse(null);
        boolean hasPassword = user != null && user.hasPassword();
        boolean matches = passwordEncoder.matches(password == null ? "" : password,
                hasPassword ? user.getPasswordHash() : dummyHash);

        // A locked account is only reported to whoever gives the right password; anyone else
        // gets the usual answer, so the lock does not reveal that the account exists.
        boolean locked = user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
        if (locked && matches) {
            throw AuthException.tooManyAttempts(Duration.between(now, user.getLockedUntil()).toSeconds() + 1);
        }
        if (locked) {
            rateLimiter.record(identifierKey, LOGIN_WINDOW);
            throw AuthException.invalidCredentials();
        }
        if (!hasPassword || !matches) {
            rateLimiter.record(identifierKey, LOGIN_WINDOW);
            if (hasPassword) {
                appUserRepository.incrementFailedLogins(user.getId());
                if (appUserRepository.lockIfTooManyFailures(user.getId(), MAX_FAILED_LOGINS, now.plus(LOCK_DURATION)) > 0) {
                    log.warn("Account {} locked for {} after {} wrong passwords", user.getId(), LOCK_DURATION, MAX_FAILED_LOGINS);
                }
            }
            throw AuthException.invalidCredentials();
        }

        // Past this point the password is right, so revealing the account state is fine
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please confirm your e-mail address first");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account is suspended");
        }

        rateLimiter.reset(identifierKey);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        return appUserRepository.save(user);
    }

    // ---------------------------------------------------------------- passwords

    public void forgotPassword(String rawEmail, String clientIp) {
        String email = CredentialRules.normalizeEmail(rawEmail);
        limitMailRequest("forgot", clientIp, email);
        appUserRepository.findByEmail(email)
                .filter(user -> user.getStatus() != UserStatus.SUSPENDED)
                .ifPresent(user -> {
                    String token = userTokenService.issue(user.getId(), UserTokenType.PASSWORD_RESET);
                    events.publishEvent(new PasswordResetRequested(email, token));
                });
    }

    /**
     * Sets a new password from an e-mailed link and returns the user id, so the caller can
     * sign out all of that user's sessions. The link also proves the address is theirs.
     */
    public Long resetPassword(String rawToken, String newPassword) {
        // Checked first, so a password that is too short does not use up the link
        CredentialRules.checkPassword(newPassword);
        Long userId = userTokenService.consume(rawToken, UserTokenType.PASSWORD_RESET);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidInputException(UserTokenService.INVALID_LINK));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account is suspended");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        markEmailVerified(user);
        appUserRepository.save(user);
        return userId;
    }

    /**
     * Requires the current password. An account without one (Google-only, or not migrated from
     * Firebase yet) must use the reset e-mail: otherwise whoever holds a stolen session could
     * add a password and keep access after the session is revoked.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        String key = "change-password:" + userId;
        if (!rateLimiter.tryAcquire(key, PASSWORD_CHANGES_PER_USER, LOGIN_WINDOW)) {
            throw AuthException.tooManyAttempts(rateLimiter.secondsUntilReset(key));
        }
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required"));
        if (!user.hasPassword()) {
            throw new BusinessRuleException("This account has no password yet: use the password reset e-mail to set one");
        }
        if (currentPassword == null || tooLong(currentPassword)
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "WRONG_PASSWORD", "The current password is wrong");
        }
        CredentialRules.checkPassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    // ---------------------------------------------------------------- username

    /**
     * Lets an account without a username (e.g. one created with Google) choose one. It is
     * chosen once; changing it later is not offered.
     */
    public AppUser chooseUsername(Long userId, String rawUsername) {
        String username = CredentialRules.normalizeUsername(rawUsername);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required"));
        if (user.getUsername() != null) {
            throw new AuthException(HttpStatus.CONFLICT, "USERNAME_ALREADY_SET", "This account already has a username");
        }
        if (appUserRepository.existsByUsername(username)) {
            throw new AuthException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "This username is already taken");
        }
        // Conditional update: of two concurrent requests, only the first sets it
        if (appUserRepository.setUsernameIfNone(userId, username) == 0) {
            throw new AuthException(HttpStatus.CONFLICT, "USERNAME_ALREADY_SET", "This account already has a username");
        }
        user.setUsername(username);
        return user;
    }

    // ---------------------------------------------------------------- helpers

    private static boolean tooLong(String password) {
        return password != null && password.length() > CredentialRules.PASSWORD_MAX_LENGTH * 2;
    }

    private static void markEmailVerified(AppUser user) {
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }
    }

    /**
     * Limits the requests that send an e-mail, per IP and per address, so the endpoints cannot
     * be used to flood an inbox. Exceeding the per-address limit is silent (same answer as
     * success), otherwise it would reveal that the address was just used.
     */
    private void limitMailRequest(String action, String clientIp, String email) {
        String ipKey = action + "-ip:" + clientIp;
        if (!rateLimiter.tryAcquire(ipKey, MAIL_REQUESTS_PER_IP, MAIL_WINDOW)) {
            throw AuthException.tooManyAttempts(rateLimiter.secondsUntilReset(ipKey));
        }
        if (!rateLimiter.tryAcquire(action + "-mail:" + email, MAIL_REQUESTS_PER_ADDRESS, MAIL_WINDOW)) {
            throw new SilentlyIgnored();
        }
    }

    /** Thrown to stop a request that must still look successful to the caller. */
    public static class SilentlyIgnored extends RuntimeException {
        SilentlyIgnored() {
            super(null, null, false, false);
        }
    }
}
