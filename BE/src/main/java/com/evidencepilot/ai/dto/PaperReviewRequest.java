package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /review/paper}.
 *
 * <p>Swagger constraints:
 * <ul>
 *   <li>{@code paper_id}     – required, min 1 char (ID returned by the AI's own paper registry)</li>
 *   <li>{@code target_style} – optional (string | null);
 *       enum: conference | article | magazine | report | thesis | unknown</li>
 *   <li>{@code use_ai}       – required, boolean, default false</li>
 * </ul>
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaperReviewRequest(

        @JsonProperty("paper_id")
        String paperId,

        @JsonProperty("target_style")
        String targetStyle,

        @JsonProperty("use_ai")
        boolean useAi
) {
    /**
     * Convenience factory: review without a target style preference, AI disabled.
     */
    public static PaperReviewRequest of(String paperId) {
        return new PaperReviewRequest(paperId, null, false);
    }

    /**
     * Convenience factory: review with an explicit target style, AI enabled.
     */
    public static PaperReviewRequest of(String paperId, String targetStyle, boolean useAi) {
        return new PaperReviewRequest(paperId, targetStyle, useAi);
    }
}
