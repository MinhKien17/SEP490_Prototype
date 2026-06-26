package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.EvidenceEdgeResponse;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.EvidenceEdge;
import java.util.UUID;

public class EvidenceMapper {

    public static ClaimResponse toClaimResponse(Claim entity) {
        if (entity == null)
            return null;
        UUID projectId = null;
        if (entity.getProject() != null) {
            projectId = entity.getProject().getId();
        }
        UUID sectionId = null;
        if (entity.getSection() != null) {
            sectionId = entity.getSection().getId();
        }
        return new ClaimResponse(
                entity.getId(),
                projectId,
                sectionId,
                entity.getContent(),
                entity.getAiConfidenceScore(),
                entity.getClaimVersion(),
                entity.isActive(),
                entity.getCreatedAt());
    }

    public static AiSuggestionResponse toAiSuggestionResponse(AiSuggestion entity) {
        if (entity == null)
            return null;
        UUID claimId = null;
        if (entity.getClaim() != null) {
            claimId = entity.getClaim().getId();
        }
        UUID documentChunkId = null;
        if (entity.getDocumentChunk() != null) {
            documentChunkId = entity.getDocumentChunk().getId();
        }
        return new AiSuggestionResponse(
                entity.getId(),
                claimId,
                documentChunkId,
                entity.getStatus().name(),
                entity.getScore(),
                entity.getExplanation(),
                entity.getClaimVersion(),
                entity.getCreatedAt());
    }

    public static EvidenceEdgeResponse toEvidenceEdgeResponse(EvidenceEdge entity) {
        if (entity == null)
            return null;
        UUID claimId = null;
        if (entity.getClaim() != null) {
            claimId = entity.getClaim().getId();
        }
        UUID documentChunkId = null;
        if (entity.getDocumentChunk() != null) {
            documentChunkId = entity.getDocumentChunk().getId();
        }
        return new EvidenceEdgeResponse(
                entity.getId(),
                claimId,
                documentChunkId,
                entity.getVerdict(),
                entity.getConfidenceScore(),
                entity.getExplanation(),
                entity.getCreatedAt());
    }
}
