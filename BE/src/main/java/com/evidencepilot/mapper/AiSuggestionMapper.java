package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.model.AiSuggestion;
import java.time.LocalDateTime;

public class AiSuggestionMapper {

    public static AiSuggestionResponse toAiSuggestionResponse(AiSuggestion entity) {
        if (entity == null) return null;
        return new AiSuggestionResponse(
            entity.getId(),
            entity.getClaim().getId(),
            entity.getDocumentChunk().getId(),
            entity.getStatus().name(),
            entity.getScore(),
            entity.getExplanation(),
            entity.getClaimVersion(),
            entity.getCreatedAt()
        );
    }
}