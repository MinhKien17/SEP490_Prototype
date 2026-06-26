package com.evidencepilot.dto.response;

import java.util.UUID;

public record ClaimMatch(
    String sourceId,
    String filename,
    UUID chunkId,
    Integer page,
    String excerpt,
    Float score,
    String suitability,
    String explanation
) {}
