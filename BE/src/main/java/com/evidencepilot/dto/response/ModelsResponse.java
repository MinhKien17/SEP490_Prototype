package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for {@code GET /ai/models}.
 */
public record ModelsResponse(

        @JsonProperty("models")
        List<ModelSummary> models
) {}
