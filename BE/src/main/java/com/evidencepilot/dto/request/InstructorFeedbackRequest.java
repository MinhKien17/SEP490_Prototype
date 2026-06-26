package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InstructorFeedbackRequest(
    @NotNull UUID sectionId,
    String lineReference,
    @NotBlank String content
) {}
