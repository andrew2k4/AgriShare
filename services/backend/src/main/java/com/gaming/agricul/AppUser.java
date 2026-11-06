package com.gaming.agricul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Set only for accounts created through Firebase; kept to migrate them, then dropped. */
    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    /** Lower-cased. Optional: accounts migrated from Firebase choose one at their next login. */
    @Column(unique = true, length = 30)
    private String username;

    /** Lower-cased. */
    @Column(unique = true)
    private String email;

    private String displayName;

    /** Null for accounts that only sign in with Google (or not yet migrated from Firebase). */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public static AppUser create(String email, String displayName, Role role, UserStatus status) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(status);
        user.setCreatedAt(Instant.now());
        return user;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }
}
