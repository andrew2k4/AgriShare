package com.gaming.agricul.repository;

import com.gaming.agricul.Lease;
import com.gaming.agricul.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, Long> {

    List<Lease> findByAssetId(Long assetId);

    Optional<Lease> findFirstByAssetIdAndStatus(Long assetId, LeaseStatus status);
}
