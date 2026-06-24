package com.evidencepilot.service.impl;

import com.evidencepilot.client.ai.AiModelClient;
import com.evidencepilot.client.ai.AiModelClient.AiApiException;
import com.evidencepilot.dto.request.ClaimAnalysisRequest;
import com.evidencepilot.dto.request.ClaimMatchRequest;
import com.evidencepilot.dto.response.ClaimAnalysisResponse;
import com.evidencepilot.dto.response.ClaimMatchResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.EvidenceEdge;
import com.evidencepilot.model.SourceChunk;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.EvidenceEdgeRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.service.AiAnalysisService;
import com.evidencepilot.service.ClaimMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final AiModelClient aiModelClient;
    private final ClaimMatchingService claimMatchingService;
    private final ClaimRepository claimRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final EvidenceEdgeRepository evidenceEdgeRepository;

    @Override
    @Transactional
    public Claim analyzeAndPersist(Claim claim) {
        ClaimMatchResponse matchResponse = claimMatchingService.matchClaim(claim, 5);
        if (!matchResponse.hasMatches()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "No matching sources found for claim: " + claim.getId());
        }

        var bestMatch = matchResponse.matches().get(0);
        return analyzeAndPersist(claim, bestMatch.sourceId(), bestMatch.excerpt(), bestMatch.filename());
    }

    @Override
    @Transactional
    public Claim analyzeAndPersist(Claim claim, String sourceId, String excerpt, String title) {
        ClaimAnalysisRequest request = new ClaimAnalysisRequest(claim.getContent(), sourceId, excerpt, title);
        ClaimAnalysisResponse response;
        try {
            response = aiModelClient.processClaim(request);
        } catch (AiApiException e) {
            log.error("AI analysis failed for claim {}: {}", claim.getId(), e.getMessage());
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "AI analysis service unavailable", e);
        }

        BigDecimal confidence = response.confidence();
        if (confidence == null) {
            confidence = BigDecimal.ZERO;
        }
        claim.setAiConfidenceScore(confidence);
        Claim saved = claimRepository.save(claim);

        String matchedSourceId = (response.matchedSourceIds() != null && !response.matchedSourceIds().isEmpty())
                ? response.matchedSourceIds().get(0)
                : sourceId;

        saveEvidenceEdge(saved, matchedSourceId, response.verdict(), confidence, response.explanation(),
                response.missingEvidence());

        return saved;
    }

    private void saveEvidenceEdge(Claim claim, String sourceId, String verdict,
                                  BigDecimal confidence, String explanation, List<String> missingEvidence) {
        SourceChunk chunk = sourceChunkRepository.findBySourceId(Integer.valueOf(sourceId)).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Source chunk not found for sourceId: " + sourceId));

        EvidenceEdge edge = new EvidenceEdge();
        edge.setClaim(claim);
        edge.setSourceChunk(chunk);
        edge.setVerdict(verdict);
        edge.setConfidenceScore(confidence);
        edge.setExplanation(explanation);
        edge.setMissingEvidence(missingEvidence != null ? String.join("; ", missingEvidence) : null);

        evidenceEdgeRepository.save(edge);
    }
}
