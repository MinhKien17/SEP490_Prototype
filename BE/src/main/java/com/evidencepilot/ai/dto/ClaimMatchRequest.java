package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /match/claim}.
 *
 * <p>Swagger constraints:
 * <ul>
 *   <li>{@code claim} – required, 1–4000 chars</li>
 *   <li>{@code top_k} – optional, 1–10, default 5 (server-side default applies when omitted)</li>
 * </ul>
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClaimMatchRequest(

        @JsonProperty("claim")
        String claim,

        @JsonProperty("top_k")
        Integer topK
) {
    /** Convenience factory: match with the server-side default top_k (5). */
    public static ClaimMatchRequest of(String claim) {
        return new ClaimMatchRequest(claim, null);
    }

    /** Convenience factory: match with an explicit top_k. */
    public static ClaimMatchRequest of(String claim, int topK) {
        return new ClaimMatchRequest(claim, topK);
    }
}
