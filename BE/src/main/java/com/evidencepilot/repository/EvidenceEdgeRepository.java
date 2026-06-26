package com.evidencepilot.repository;

import com.evidencepilot.model.EvidenceEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EvidenceEdgeRepository extends JpaRepository<EvidenceEdge, UUID> {
    List<EvidenceEdge> findByClaimId(UUID claimId);
    List<EvidenceEdge> findByDocumentChunkId(UUID documentChunkId);
    List<EvidenceEdge> findByClaimIdAndDocumentChunkId(UUID claimId, UUID documentChunkId);
}
