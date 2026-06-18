package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single model entry returned by {@code GET /ai/models}.
 */
public record ModelSummary(

        @JsonProperty("name")
        String name
) {}
