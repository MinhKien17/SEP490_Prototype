package com.evidencepilot.service;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.ai.dto.ClaimAnalysisRequest;
import com.evidencepilot.ai.dto.ClaimAnalysisResponse;
import com.evidencepilot.ai.dto.ClaimMatch;
import com.evidencepilot.ai.dto.ClaimMatchResponse;
import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Graph;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.GraphRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private GraphRepository graphRepository;

    @Mock
    private ClaimMatchingService claimMatchingService;

    @InjectMocks
    private AiAnalysisService aiAnalysisService;

    @Test
    void analyzeAndPersistMatchesTopSourceThenPersistsConfidenceAndGraph() {
        Claim claim = claim(42, "Smoking increases cardiovascular risk.");
        ClaimMatch match = new ClaimMatch(
                "source-1",
                "paper.pdf",
                "chunk-7",
                3,
                "Cardiovascular risk was higher among smokers.",
                new BigDecimal("0.91"),
                "strong",
                "Directly related"
        );
        ClaimAnalysisResponse response = new ClaimAnalysisResponse(
                "supported",
                new BigDecimal("0.8700"),
                List.of("source-1"),
                List.of(),
                "The cited excerpt supports the claim."
        );

        when(claimMatchingService.matchClaim(claim, 1))
                .thenReturn(new ClaimMatchResponse(claim.getContent(), List.of(match)));
        when(aiModelClient.processClaim(any(ClaimAnalysisRequest.class))).thenReturn(response);
        when(claimRepository.save(claim)).thenReturn(claim);
        when(graphRepository.findByClaimId(42)).thenReturn(Optional.empty());

        Claim updated = aiAnalysisService.analyzeAndPersist(claim);

        assertThat(updated.getAiConfidenceScore()).isEqualByComparingTo("0.8700");

        ArgumentCaptor<ClaimAnalysisRequest> analysisRequest =
                ArgumentCaptor.forClass(ClaimAnalysisRequest.class);
        verify(aiModelClient).processClaim(analysisRequest.capture());
        assertThat(analysisRequest.getValue().sourceId()).isEqualTo("source-1");
        assertThat(analysisRequest.getValue().excerpt())
                .isEqualTo("Cardiovascular risk was higher among smokers.");

        ArgumentCaptor<Graph> graph = ArgumentCaptor.forClass(Graph.class);
        verify(graphRepository).save(graph.capture());
        assertThat(graph.getValue().getClaim()).isSameAs(claim);
        assertThat(graph.getValue().getGraphData())
                .containsEntry("verdict", "supported")
                .containsEntry("confidence", new BigDecimal("0.8700"))
                .containsEntry("_source_id_used", "source-1");
    }

    @Test
    void analyzeAndPersistRejectsWhenAiReturnsNoMatches() {
        Claim claim = claim(42, "Unsupported claim.");
        when(claimMatchingService.matchClaim(claim, 1))
                .thenReturn(new ClaimMatchResponse(claim.getContent(), List.of()));

        assertThatThrownBy(() -> aiAnalysisService.analyzeAndPersist(claim))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(aiModelClient, never()).processClaim(any());
        verify(claimRepository, never()).save(any());
        verify(graphRepository, never()).save(any());
    }

    @Test
    void analyzeAndPersistWrapsAiProcessErrorsAsBadGateway() {
        Claim claim = claim(42, "Claim text.");
        when(aiModelClient.processClaim(any(ClaimAnalysisRequest.class)))
                .thenThrow(new AiModelClient.AiApiException("POST /process/claim", 500));

        assertThatThrownBy(() ->
                aiAnalysisService.analyzeAndPersist(claim, "source-1", "excerpt", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        verify(claimRepository, never()).save(any());
        verify(graphRepository, never()).save(any());
    }

    private static Claim claim(Integer id, String content) {
        Claim claim = new Claim();
        claim.setId(id);
        claim.setContent(content);
        return claim;
    }
}
