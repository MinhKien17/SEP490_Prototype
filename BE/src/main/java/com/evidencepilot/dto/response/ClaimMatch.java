package com.evidencepilot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * A single match entry inside {@link ClaimMatchResponse}.
 *
 * <p>Swagger field summary:
 * <ul>
 *   <li>{@code source_id}  – required</li>
 *   <li>{@code filename}   – required</li>
 *   <li>{@code chunk_id}   – required</li>
 *   <li>{@code page}       – optional (integer | null)</li>
 *   <li>{@code excerpt}    – required</li>
 *   <li>{@code score}      – required; 0.0 – 1.0</li>
 *   <li>{@code suitability}– required; enum: strong | medium | weak</li>
 *   <li>{@code explanation}– required</li>
 * </ul>
 * </p>
 */
public record ClaimMatch(

        @JsonProperty("source_id")
        String sourceId,

        @JsonProperty("filename")
        String filename,

        @JsonProperty("chunk_id")
        String chunkId,

        @JsonProperty("page")
        Integer page,

        @JsonProperty("excerpt")
        String excerpt,

        @JsonProperty("score")
        BigDecimal score,

        @JsonProperty("suitability")
        String suitability,

        @JsonProperty("explanation")
        String explanation
) {}
