package com.gaming.agricul.repository;

import com.gaming.agricul.UserToken;
import com.gaming.agricul.UserTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    /**
     * Marks a token as used only if nobody did it first. Returns 0 when another request
     * consumed it concurrently, so the same link can never be used twice.
     */
    @Modifying
    @Query("UPDATE UserToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    /** Voids the still-valid tokens of a user, e.g. when a new link replaces them. */
    @Modifying
    @Query("UPDATE UserToken t SET t.usedAt = :now "
            + "WHERE t.userId = :userId AND t.type = :type AND t.usedAt IS NULL")
    int invalidateUnused(@Param("userId") Long userId, @Param("type") UserTokenType type,
                         @Param("now") Instant now);
}
