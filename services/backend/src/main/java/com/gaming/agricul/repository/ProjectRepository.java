package com.gaming.agricul.repository;

import com.gaming.agricul.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerUid(String ownerUid);

    /** Moves every project of one owner key to another, e.g. when a Firebase UID is detached. */
    @Modifying
    @Query("UPDATE Project p SET p.ownerUid = :to WHERE p.ownerUid = :from")
    int reassignOwner(@Param("from") String from, @Param("to") String to);
}
