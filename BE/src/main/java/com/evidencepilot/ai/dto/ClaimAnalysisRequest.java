package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /process/claim}.
 *
 * <p>Swagger constraints:
 * <ul>
 *   <li>{@code claim}     – required, 1–4000 chars</li>
 *   <li>{@code source_id} – required, 1–120 chars (ID of a source known to the AI service)</li>
 *   <li>{@code excerpt}   – required, 1–4000 chars (raw text excerpt from that source)</li>
 *   <li>{@code title}     – optional (string | null), max 300 chars</li>
 * </ul>
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClaimAnalysisRequest(

        @JsonProperty("claim")
        String claim,

        @JsonProperty("source_id")
        String sourceId,

        @JsonProperty("excerpt")
        String excerpt,

        @JsonProperty("title")
        String title
) {}
