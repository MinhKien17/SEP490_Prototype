package com.evidencepilot.service;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import java.util.List;
import java.util.UUID;

public interface ClaimMatchingService {

    List<AiSuggestionResponse> matchClaim(UUID claimId, UUID projectId);
}
