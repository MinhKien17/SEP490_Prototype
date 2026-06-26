package com.evidencepilot.dto.request;

import java.util.UUID;

public record CollectionRequest(
    String name,
    String description,
    UUID projectId
) {}
