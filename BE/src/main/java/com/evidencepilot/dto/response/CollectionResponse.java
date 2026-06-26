package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CollectionResponse(
    UUID id,
    String name,
    String description,
    UUID projectId,
    LocalDateTime createdAt
) {}
