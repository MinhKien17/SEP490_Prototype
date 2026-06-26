package com.evidencepilot.controller;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.EvidenceEdgeResponse;
import com.evidencepilot.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping
    public List<ClaimResponse> getAllClaims() {
        return claimService.getAllClaims();
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaimById(@PathVariable UUID id) {
        return claimService.getClaimById(id);
    }

    @GetMapping("/by-project/{projectId}")
    public List<ClaimResponse> getClaimsByProject(@PathVariable UUID projectId) {
        return claimService.getClaimsByProject(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse createClaim(@Valid @RequestBody ClaimCreationRequest request) {
        return claimService.createClaim(request);
    }

    @PutMapping("/{id}")
    public ClaimResponse updateClaim(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Float aiConfidenceScore = body.get("aiConfidenceScore") != null
                ? ((Number) body.get("aiConfidenceScore")).floatValue()
                : null;
        return claimService.updateClaim(id, content, aiConfidenceScore);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClaim(@PathVariable UUID id) {
        claimService.deleteClaim(id);
    }

    @GetMapping("/{id}/suggestions")
    public List<AiSuggestionResponse> getSuggestions(@PathVariable UUID id) {
        return claimService.getSuggestionsForClaim(id);
    }

    @PostMapping("/{id}/suggestions")
    @ResponseStatus(HttpStatus.CREATED)
    public AiSuggestionResponse createSuggestion(@PathVariable UUID id,
                                                  @RequestParam UUID documentChunkId,
                                                  @RequestParam Float score,
                                                  @RequestParam String explanation) {
        return claimService.createSuggestion(id, documentChunkId, score, explanation);
    }

    @PostMapping("/suggestions/{suggestionId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptSuggestion(@PathVariable UUID suggestionId) {
        claimService.acceptSuggestion(suggestionId);
    }

    @PostMapping("/suggestions/{suggestionId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectSuggestion(@PathVariable UUID suggestionId) {
        claimService.rejectSuggestion(suggestionId);
    }

    @GetMapping("/{id}/mappings")
    public List<ClaimEvidenceMappingResponse> getMappings(@PathVariable UUID id) {
        return claimService.getMappingsForClaim(id);
    }

    @GetMapping("/{id}/edges")
    public List<EvidenceEdgeResponse> getEdges(@PathVariable UUID id) {
        return claimService.getEdgesForClaim(id);
    }
}
