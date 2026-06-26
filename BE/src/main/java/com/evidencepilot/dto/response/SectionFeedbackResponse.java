package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SectionFeedbackResponse(
    UUID id,
    UUID sectionId,
    UUID authorId,
    String lineReference,
    String content,
    boolean resolved,
    LocalDateTime createdAt
) {}
