package com.evidencepilot.repository;

import com.evidencepilot.model.ClaimEvidenceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClaimEvidenceMappingRepository extends JpaRepository<ClaimEvidenceMapping, UUID> {
    List<ClaimEvidenceMapping> findByClaimId(UUID claimId);
    List<ClaimEvidenceMapping> findByDocumentChunkId(UUID documentChunkId);
    List<ClaimEvidenceMapping> findByClaimIdAndDocumentChunkId(UUID claimId, UUID documentChunkId);
}
