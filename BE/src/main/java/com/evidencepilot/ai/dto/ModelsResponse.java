package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for {@code GET /ai/models}.
 */
public record ModelsResponse(

        @JsonProperty("models")
        List<ModelSummary> models
) {}
