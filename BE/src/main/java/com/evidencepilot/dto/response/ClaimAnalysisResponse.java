package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response body for {@code POST /process/claim}.
 *
 * <p>Swagger field summary:
 * <ul>
 *   <li>{@code verdict}            – required; enum: supported | partially_supported | unsupported | unclear</li>
 *   <li>{@code confidence}         – required; 0.0 – 1.0</li>
 *   <li>{@code matched_source_ids} – optional array of matched source IDs</li>
 *   <li>{@code missing_evidence}   – optional array of missing-evidence descriptions</li>
 *   <li>{@code explanation}        – required; 1–2000 chars</li>
 * </ul>
 * </p>
 */
public record ClaimAnalysisResponse(

        @JsonProperty("verdict")
        String verdict,

        @JsonProperty("confidence")
        BigDecimal confidence,

        @JsonProperty("matched_source_ids")
        List<String> matchedSourceIds,

        @JsonProperty("missing_evidence")
        List<String> missingEvidence,

        @JsonProperty("explanation")
        String explanation
) {}
