package com.evidencepilot.dto.response;

import java.util.UUID;

public record DocumentTextResponse(
    UUID id,
    UUID documentId,
    String extractedText,
    String extractionMethod
) {}
