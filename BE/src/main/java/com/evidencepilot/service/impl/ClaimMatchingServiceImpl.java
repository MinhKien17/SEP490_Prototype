package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.ClaimMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimMatchingServiceImpl implements ClaimMatchingService {

    private final ClaimRepository claimRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final ClaimMapper claimMapper;

    @Override
    @Transactional
    public List<AiSuggestionResponse> matchClaim(UUID claimId, UUID projectId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(claimId, "Claim"));

        List<Document> projectDocuments = documentRepository.findByProjectId(projectId);
        List<DocumentChunk> chunks = projectDocuments.stream()
                .filter(doc -> doc.isActive() && doc.getDocType() == DocumentType.SOURCE)
                .flatMap(doc -> documentChunkRepository.findByDocumentId(doc.getId()).stream())
                .toList();

        List<AiSuggestion> suggestions = chunks.stream()
                .map(chunk -> buildSuggestion(claim, chunk))
                .toList();

        List<AiSuggestion> saved = aiSuggestionRepository.saveAll(suggestions);

        log.info("Created {} suggestions for claim {}", saved.size(), claimId);
        return saved.stream()
                .map(claimMapper::toAiSuggestionResponse)
                .toList();
    }

    private AiSuggestion buildSuggestion(Claim claim, DocumentChunk chunk) {
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setClaim(claim);
        suggestion.setDocumentChunk(chunk);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setScore(0.5f);
        suggestion.setExplanation("Auto-matched chunk " + chunk.getChunkIndex());
        suggestion.setClaimVersion(claim.getClaimVersion());
        suggestion.setCreatedAt(LocalDateTime.now());
        return suggestion;
    }
}
