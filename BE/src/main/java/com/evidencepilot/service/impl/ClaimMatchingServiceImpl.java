package com.evidencepilot.service.impl;

import com.evidencepilot.client.ai.AiModelClient;
import com.evidencepilot.client.ai.AiModelClient.AiApiException;
import com.evidencepilot.dto.request.ClaimMatchRequest;
import com.evidencepilot.dto.response.ClaimMatchResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.service.ClaimMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimMatchingServiceImpl implements ClaimMatchingService {

    private final AiModelClient aiModelClient;

    @Override
    public ClaimMatchResponse matchClaim(Claim claim, int topK) {
        ClaimMatchRequest request = ClaimMatchRequest.of(claim.getContent(), topK);
        try {
            return aiModelClient.matchClaim(request);
        } catch (AiApiException e) {
            log.error("Claim matching failed for claim {}: {}", claim.getId(), e.getMessage());
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Claim matching service unavailable", e);
        }
    }
}
