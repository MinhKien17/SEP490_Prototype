package com.evidencepilot.dto.response;

import java.util.UUID;

public record DocumentChunkResponse(
    UUID id,
    UUID documentId,
    Integer chunkIndex,
    String text,
    boolean active
) {}
