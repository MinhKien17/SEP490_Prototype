package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.ai.AiModelClient.AiApiException;
import com.evidencepilot.ai.dto.*;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Graph;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.GraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orchestrates the full AI claim-analysis pipeline.
 *
 * <h2>Two-phase workflow</h2>
 * <ol>
 *   <li><b>Match</b> – calls {@code POST /match/claim} with the claim text to let
 *       the AI locate relevant source chunks it already knows about.  The best
 *       match supplies the {@code source_id} and {@code excerpt} required by the
 *       next step.</li>
 *   <li><b>Process</b> – calls {@code POST /process/claim} with the claim, the
 *       winning {@code source_id}, and its {@code excerpt}.  The response gives us
 *       a {@code verdict} enum, a numeric {@code confidence} score, and an
 *       {@code explanation}.</li>
 * </ol>
 *
 * <h2>Persistence</h2>
 * <ul>
 *   <li>{@code Claim.aiConfidenceScore} ← {@code confidence}</li>
 *   <li>{@code Graph.graphData}         ← full response payload as a JSON document</li>
 * </ul>
 *
 * <p>Both writes happen inside a single {@link Transactional} boundary so a partial
 * failure cannot leave the DB in an inconsistent state.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiModelClient aiModelClient;
    private final ClaimRepository claimRepository;
    private final GraphRepository graphRepository;
    private final ClaimMatchingService claimMatchingService;

    // ── Primary pipeline ───────────────────────────────────────────────────────

    /**
     * Full two-phase pipeline: match → process → persist.
     *
     * <p>Automatically discovers the best-matching source by calling
     * {@code POST /match/claim} first.  Use the overload below if the caller
     * already knows the exact {@code source_id} and {@code excerpt}.</p>
     *
     * @param claim the Claim to analyse (must already be persisted in the DB)
     * @return the updated Claim with {@code aiConfidenceScore} populated
     * @throws ResponseStatusException 404 if no matching sources are found in the AI system,
     *                                 502 on any AI API error
     */
    @Transactional
    public Claim analyzeAndPersist(Claim claim) {
        // ── Phase 1: find the best-matching source ─────────────────────────────
        log.info("Phase 1 – matching sources for claim {}", claim.getId());

        ClaimMatchResponse matchResponse = claimMatchingService.matchClaim(claim, 1);

        if (!matchResponse.hasMatches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No persisted source chunks found for claim " + claim.getId()
                    + ". Upload at least one source to this project before analysing.");
        }

        ClaimMatch topMatch = matchResponse.matches().get(0);
        log.info("Phase 1 complete – top match: sourceId={}, suitability={}, score={}",
                topMatch.sourceId(), topMatch.suitability(), topMatch.score());

        // ── Phase 2: deep analysis with the winning source ─────────────────────
        return analyzeAndPersist(claim, topMatch.sourceId(), topMatch.excerpt(), null);
    }

    /**
     * Skips the match phase and calls {@code POST /process/claim} directly.
     *
     * <p>Use this overload when the caller already knows the {@code source_id}
     * (e.g. an instructor manually selecting a specific reference) and wants to
     * avoid the extra round-trip to the AI service.</p>
     *
     * @param claim    the Claim to analyse
     * @param sourceId the source identifier known to the AI service
     * @param excerpt  the relevant text excerpt from that source
     * @param title    optional title for the source (may be {@code null})
     * @return the updated Claim with {@code aiConfidenceScore} populated
     */
    @Transactional
    public Claim analyzeAndPersist(Claim claim, String sourceId, String excerpt, String title) {
        // ── Phase 2: process/claim ─────────────────────────────────────────────
        log.info("Phase 2 – processing claim {} against sourceId={}", claim.getId(), sourceId);

        ClaimAnalysisRequest request = new ClaimAnalysisRequest(
                claim.getContent(),
                sourceId,
                excerpt,
                title
        );

        ClaimAnalysisResponse response;
        try {
            response = aiModelClient.processClaim(request);
        } catch (AiApiException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI process/claim call failed for claim " + claim.getId() + ": " + e.getMessage(), e);
        }

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI process/claim returned null for claim " + claim.getId());
        }

        log.info("Phase 2 complete – verdict={}, confidence={}", response.verdict(), response.confidence());

        // ── Persist: update Claim.aiConfidenceScore ────────────────────────────
        claim.setAiConfidenceScore(response.confidence());
        Claim updatedClaim = claimRepository.save(claim);

        // ── Persist: upsert Graph with full response JSON ──────────────────────
        Map<String, Object> graphData = buildGraphData(response, sourceId);
        Graph graph = graphRepository.findByClaimId(claim.getId()).orElse(new Graph());
        graph.setClaim(updatedClaim);
        graph.setGraphData(graphData);
        graphRepository.save(graph);

        log.info("Persisted AI results for claim {}", claim.getId());
        return updatedClaim;
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /**
     * Converts a {@link ClaimAnalysisResponse} to the {@code Map<String,Object>}
     * format expected by {@link Graph#graphData}.
     *
     * <p>All Swagger response fields are preserved so the frontend can display
     * the full AI rationale without a second round-trip.</p>
     */
    private static Map<String, Object> buildGraphData(ClaimAnalysisResponse response,
                                                      String sourceIdUsed) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("verdict", response.verdict());
        map.put("confidence", response.confidence());
        map.put("explanation", response.explanation());
        map.put("matched_source_ids", response.matchedSourceIds());
        map.put("missing_evidence", response.missingEvidence());
        // Record which source was used so the result is self-documenting in the DB
        map.put("_source_id_used", sourceIdUsed);
        return map;
    }
}
