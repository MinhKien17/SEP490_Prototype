package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SubmitReviewRequest(
    @NotNull UUID instructorId
) {}
