package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import org.springframework.web.server.ResponseStatusException;

public interface AiAnalysisService {

    /**
     * Full two-phase pipeline: match → process → persist.
     *
     * @param claim the Claim to analyse (must already be persisted in the DB)
     * @return the updated Claim with {@code aiConfidenceScore} populated
     * @throws ResponseStatusException 404 if no matching sources are found in the AI system,
     *                                 503 on any AI API error
     */
    Claim analyzeAndPersist(Claim claim);

    /**
     * Skips the match phase and calls {@code POST /process/claim} directly.
     *
     * @param claim    the Claim to analyse
     * @param sourceId the source identifier known to the AI service
     * @param excerpt  the relevant text excerpt from that source
     * @param title    optional title for the source (may be {@code null})
     * @return the updated Claim with {@code aiConfidenceScore} populated
     */
    Claim analyzeAndPersist(Claim claim, String sourceId, String excerpt, String title);
}
