package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ProjectCreateRequest(
    @NotBlank String title,
    String description,
    String targetStandard
) {}
