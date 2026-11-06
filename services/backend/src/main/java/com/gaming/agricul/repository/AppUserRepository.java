package com.gaming.agricul.repository;

import com.gaming.agricul.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * E-mail and username lookups expect an already lower-cased value (see CredentialRules) and
 * compare on lower(...) so they use the uq_app_users_email / uq_app_users_username indexes.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByFirebaseUid(String firebaseUid);

    @Query("SELECT u FROM AppUser u WHERE lower(u.email) = :email")
    Optional<AppUser> findByEmail(@Param("email") String normalizedEmail);

    @Query("SELECT u FROM AppUser u WHERE lower(u.username) = :username")
    Optional<AppUser> findByUsername(@Param("username") String normalizedUsername);

    @Query("SELECT count(u) > 0 FROM AppUser u WHERE lower(u.email) = :email")
    boolean existsByEmail(@Param("email") String normalizedEmail);

    @Query("SELECT count(u) > 0 FROM AppUser u WHERE lower(u.username) = :username")
    boolean existsByUsername(@Param("username") String normalizedUsername);
}
