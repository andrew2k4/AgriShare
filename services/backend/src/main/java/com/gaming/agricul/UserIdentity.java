package com.gaming.agricul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** An external identity (e.g. a Google account) linked to a local {@link AppUser}. */
@Entity
@Table(name = "user_identities",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_identities_provider_subject",
                columnNames = {"provider", "subject"}))
@Getter
@Setter
@NoArgsConstructor
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdentityProvider provider;

    /** The provider's stable user id (OIDC "sub" claim), never the e-mail. */
    @Column(nullable = false)
    private String subject;

    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
