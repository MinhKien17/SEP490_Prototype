package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.EvidenceEdgeResponse;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.EvidenceEdge;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ClaimMapper {

    public ClaimResponse toClaimResponse(Claim entity) {
        if (entity == null) return null;
        UUID projectId = entity.getProject() != null ? entity.getProject().getId() : null;
        UUID sectionId = entity.getSection() != null ? entity.getSection().getId() : null;
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

    public AiSuggestionResponse toAiSuggestionResponse(AiSuggestion entity) {
        if (entity == null) return null;
        UUID claimId = entity.getClaim() != null ? entity.getClaim().getId() : null;
        UUID documentChunkId = entity.getDocumentChunk() != null ? entity.getDocumentChunk().getId() : null;
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

    public EvidenceEdgeResponse toEvidenceEdgeResponse(EvidenceEdge entity) {
        if (entity == null) return null;
        UUID claimId = entity.getClaim() != null ? entity.getClaim().getId() : null;
        UUID documentChunkId = entity.getDocumentChunk() != null ? entity.getDocumentChunk().getId() : null;
        return new EvidenceEdgeResponse(
                entity.getId(),
                claimId,
                documentChunkId,
                entity.getVerdict(),
                entity.getConfidenceScore(),
                entity.getExplanation(),
                entity.getCreatedAt());
    }

    public ClaimEvidenceMappingResponse toClaimEvidenceMappingResponse(ClaimEvidenceMapping entity) {
        if (entity == null) return null;
        UUID claimId = entity.getClaim() != null ? entity.getClaim().getId() : null;
        UUID documentChunkId = entity.getDocumentChunk() != null ? entity.getDocumentChunk().getId() : null;
        UUID suggestionId = entity.getSuggestion() != null ? entity.getSuggestion().getId() : null;
        UUID createdById = entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null;
        return new ClaimEvidenceMappingResponse(
                entity.getId(),
                claimId,
                documentChunkId,
                suggestionId,
                createdById,
                entity.getStatus().name(),
                entity.getCreatedAt());
    }
}
