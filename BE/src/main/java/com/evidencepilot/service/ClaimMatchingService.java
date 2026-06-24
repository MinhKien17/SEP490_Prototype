package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import com.evidencepilot.dto.response.ClaimMatchResponse;

public interface ClaimMatchingService {

    ClaimMatchResponse matchClaim(Claim claim, int topK);
}
