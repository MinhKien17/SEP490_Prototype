package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a structural issue in one section of a paper.
 * Used in {@link PaperReviewResponse} for missing sections, weak sections,
 * and claim recommendations.
 *
 * <p>All three fields are required per the Swagger spec.</p>
 */
public record SectionIssue(

        @JsonProperty("section")
        String section,

        @JsonProperty("issue")
        String issue,

        @JsonProperty("recommendation")
        String recommendation
) {}
