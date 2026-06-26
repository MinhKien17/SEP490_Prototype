package com.evidencepilot.repository;

import com.evidencepilot.model.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
    List<AiSuggestion> findByClaimId(UUID claimId);
    List<AiSuggestion> findByDocumentChunkId(UUID documentChunkId);
    List<AiSuggestion> findByStatusOrderByCreatedAtDesc(String status);
}
