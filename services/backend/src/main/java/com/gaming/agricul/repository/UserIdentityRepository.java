package com.gaming.agricul.repository;

import com.gaming.agricul.IdentityProvider;
import com.gaming.agricul.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndSubject(IdentityProvider provider, String subject);

    List<UserIdentity> findByUserId(Long userId);
}
