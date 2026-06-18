package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for Claim CRUD operations and AI analysis trigger.
 *
 * <p>Base path: {@code /api/claims}</p>
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final AiAnalysisService aiAnalysisService;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    @GetMapping
    public List<Claim> findAll() {
        return claimRepository.findAll();
    }

    @GetMapping("/{id}")
    public Claim findById(@PathVariable Integer id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));
    }

    @GetMapping("/by-project/{projectId}")
    public List<Claim> findByProject(@PathVariable Integer projectId) {
        return claimRepository.findByProjectId(projectId);
    }

    @PostMapping
    public ResponseEntity<Claim> create(@RequestBody Claim claim) {
        Claim saved = claimRepository.save(claim);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Claim update(@PathVariable Integer id, @RequestBody Claim claim) {
        if (!claimRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id);
        }
        claim.setId(id);
        return claimRepository.save(claim);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!claimRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found: " + id);
        }
        claimRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── AI analysis ────────────────────────────────────────────────────────────

    /**
     * Triggers the two-phase AI analysis pipeline for a single claim.
     *
     * <h3>Auto mode (no parameters)</h3>
     * <p>The service calls {@code POST /match/claim} first to locate the best
     * source in the AI system, then calls {@code POST /process/claim} with that
     * source.  Use this when you want the AI to pick the evidence automatically.</p>
     *
     * <h3>Manual mode (sourceId + excerpt provided)</h3>
     * <p>Skips the match phase and calls {@code POST /process/claim} directly with
     * the provided {@code sourceId} and {@code excerpt}.  Use this when an instructor
     * wants to pin the analysis to a specific reference.</p>
     *
     * @param id       the Claim to analyse
     * @param sourceId optional – AI-service source identifier to use directly
     * @param excerpt  optional – text excerpt from that source (required when sourceId is set)
     * @param title    optional – source title forwarded to the AI for context
     * @return the updated Claim with {@code aiConfidenceScore} set and the Graph persisted
     */
    @PostMapping("/{id}/analyze")
    public Claim analyze(
            @PathVariable Integer id,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String excerpt,
            @RequestParam(required = false) String title) {

        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found: " + id));

        boolean manualMode = sourceId != null && !sourceId.isBlank()
                          && excerpt  != null && !excerpt.isBlank();

        if (manualMode) {
            return aiAnalysisService.analyzeAndPersist(claim, sourceId, excerpt, title);
        } else {
            return aiAnalysisService.analyzeAndPersist(claim);
        }
    }
}
