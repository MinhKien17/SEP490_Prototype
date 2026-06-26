package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record EvidenceEdgeResponse(
    UUID id,
    UUID claimId,
    UUID documentChunkId,
    String verdict,
    Float confidenceScore,
    String explanation,
    LocalDateTime createdAt
) {}
