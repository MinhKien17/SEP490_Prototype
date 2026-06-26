package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.model.ClaimEvidenceMapping;
import java.time.LocalDateTime;

public class ClaimEvidenceMappingMapper {

    public static ClaimEvidenceMappingResponse toClaimEvidenceMappingResponse(ClaimEvidenceMapping entity) {
        if (entity == null) return null;
        return new ClaimEvidenceMappingResponse(
            entity.getId(),
            entity.getClaim().getId(),
            entity.getDocumentChunk().getId(),
            entity.getSuggestion() != null ? entity.getSuggestion().getId() : null,
            entity.getCreatedBy().getId(),
            entity.getStatus().name(),
            entity.getCreatedAt()
        );
    }
}
