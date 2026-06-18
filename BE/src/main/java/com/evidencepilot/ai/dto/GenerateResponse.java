package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for {@code POST /ai/generate}.
 *
 * <p>All three fields are required per the Swagger spec.</p>
 */
public record GenerateResponse(

        @JsonProperty("model")
        String model,

        @JsonProperty("response")
        String response,

        @JsonProperty("done")
        boolean done
) {}
