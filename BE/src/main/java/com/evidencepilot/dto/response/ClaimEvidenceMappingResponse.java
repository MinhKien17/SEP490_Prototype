package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClaimEvidenceMappingResponse(
    UUID id,
    UUID claimId,
    UUID documentChunkId,
    UUID suggestionId,
    UUID createdBy,
    String status,
    LocalDateTime createdAt
) {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
}
