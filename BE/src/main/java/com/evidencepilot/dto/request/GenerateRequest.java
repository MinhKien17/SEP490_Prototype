package com.evidencepilot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /ai/generate}.
 *
 * <p>Swagger constraints:
 * <ul>
 *   <li>{@code prompt} – required, 1–12 000 chars</li>
 * </ul>
 * </p>
 */
public record GenerateRequest(

        @JsonProperty("prompt")
        String prompt
) {}
