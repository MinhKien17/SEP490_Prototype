package com.evidencepilot.repository;

import com.evidencepilot.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByProjectId(UUID projectId);
    List<Claim> findBySectionId(UUID sectionId);
    List<Claim> findByProjectIdAndSectionId(UUID projectId, UUID sectionId);
    List<Claim> findBySectionIdAndProjectIdOrderByClaimVersionDesc(UUID sectionId, UUID projectId);
}
