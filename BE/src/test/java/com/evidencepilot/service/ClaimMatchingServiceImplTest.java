package com.evidencepilot.service;

import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.service.impl.ClaimMatchingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimMatchingServiceImplTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private AiSuggestionRepository aiSuggestionRepository;

    @Mock
    private ClaimMapper claimMapper;

    @Test
    void matchClaimOnlyUsesEvidenceSourceChunks() {
        UUID projectId = UUID.randomUUID();
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setClaimVersion(1);

        Document source = document(DocumentType.SOURCE);
        Document paper = document(DocumentType.PAPER);
        DocumentChunk sourceChunk = chunk(source);
        DocumentChunk paperChunk = chunk(paper);

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(documentRepository.findByProjectId(projectId)).thenReturn(List.of(source, paper));
        when(documentChunkRepository.findByDocumentId(source.getId())).thenReturn(List.of(sourceChunk));
        lenient().when(documentChunkRepository.findByDocumentId(paper.getId())).thenReturn(List.of(paperChunk));
        when(aiSuggestionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimMatchingServiceImpl service = new ClaimMatchingServiceImpl(
                claimRepository,
                documentRepository,
                documentChunkRepository,
                aiSuggestionRepository,
                claimMapper);

        service.matchClaim(claim.getId(), projectId);

        ArgumentCaptor<List<AiSuggestion>> suggestionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiSuggestionRepository).saveAll(suggestionsCaptor.capture());
        assertThat(suggestionsCaptor.getValue())
                .singleElement()
                .extracting(suggestion -> suggestion.getDocumentChunk().getDocument().getDocType())
                .isEqualTo(DocumentType.SOURCE);
        verify(documentChunkRepository, never()).findByDocumentId(paper.getId());
    }

    private Document document(DocumentType type) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setDocType(type);
        document.setActive(true);
        return document;
    }

    private DocumentChunk chunk(Document document) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(document);
        chunk.setChunkIndex(0);
        chunk.setText("Evidence text");
        chunk.setActive(true);
        return chunk;
    }
}
