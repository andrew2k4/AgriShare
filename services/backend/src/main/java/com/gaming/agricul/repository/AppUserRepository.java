package com.gaming.agricul.repository;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * E-mail and username lookups expect an already lower-cased value (see CredentialRules) and
 * compare on lower(...) so they use the uq_app_users_email / uq_app_users_username indexes.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByRole(Role role);

    @Query("SELECT u FROM AppUser u WHERE lower(u.email) = :email")
    Optional<AppUser> findByEmail(@Param("email") String normalizedEmail);

    @Query("SELECT u FROM AppUser u WHERE lower(u.username) = :username")
    Optional<AppUser> findByUsername(@Param("username") String normalizedUsername);

    @Query("SELECT count(u) > 0 FROM AppUser u WHERE lower(u.email) = :email")
    boolean existsByEmail(@Param("email") String normalizedEmail);

    @Query("SELECT count(u) > 0 FROM AppUser u WHERE lower(u.username) = :username")
    boolean existsByUsername(@Param("username") String normalizedUsername);

    /** Atomic, so concurrent wrong passwords cannot overwrite each other's count. */
    @Modifying
    @Query("UPDATE AppUser u SET u.failedLoginCount = u.failedLoginCount + 1 WHERE u.id = :id")
    int incrementFailedLogins(@Param("id") Long id);

    /** Locks the account once it has reached the limit, and restarts the count. */
    @Modifying
    @Query("UPDATE AppUser u SET u.lockedUntil = :until, u.failedLoginCount = 0 "
            + "WHERE u.id = :id AND u.failedLoginCount >= :max")
    int lockIfTooManyFailures(@Param("id") Long id, @Param("max") int max, @Param("until") Instant until);

    /** Sets the username only if the account has none yet; returns 0 otherwise. */
    @Modifying
    @Query("UPDATE AppUser u SET u.username = :username WHERE u.id = :id AND u.username IS NULL")
    int setUsernameIfNone(@Param("id") Long id, @Param("username") String username);
}
