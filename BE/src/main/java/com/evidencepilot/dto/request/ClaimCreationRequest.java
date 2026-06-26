package com.evidencepilot.dto.request;

import java.util.UUID;

public record ClaimCreationRequest(
    UUID sectionId,
    String content,
    Float aiConfidenceScore
) {}
