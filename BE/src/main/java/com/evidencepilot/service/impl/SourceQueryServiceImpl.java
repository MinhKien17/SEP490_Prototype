package com.evidencepilot.service.impl;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.SourceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceQueryServiceImpl implements SourceQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Transactional(readOnly = true)
    public List<DocumentTextResponse> getDocumentTextsByProject(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));

        if (!currentUserService.isAdmin(currentUser)) {
            currentUserService.requireProjectAccess(currentUser, project);
        }

        return documentRepository.findByProjectId(projectId).stream()
                .map(doc -> {
                    DocumentText dt = documentTextRepository.findByDocumentId(doc.getId());
                    if (dt == null) return null;
                    return new DocumentTextResponse(dt.getId(), dt.getDocument().getId(),
                            dt.getExtractedText(), dt.getExtractionMethod());
                })
                .filter(r -> r != null)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Transactional(readOnly = true)
    public DocumentTextResponse getProjectDocumentText(UUID projectId, UUID documentId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found: " + documentId));
        if (!doc.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found in this project");
        }

        DocumentText dt = documentTextRepository.findByDocumentId(documentId);
        if (dt == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Extracted text not found for document");
        }
        return new DocumentTextResponse(dt.getId(), dt.getDocument().getId(),
                dt.getExtractedText(), dt.getExtractionMethod());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'STUDENT')")
    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getDocumentChunks(UUID documentId) {
        return documentChunkRepository.findByDocumentId(documentId).stream()
                .map(chunk -> new DocumentChunkResponse(
                        chunk.getId(),
                        chunk.getDocument().getId(),
                        chunk.getChunkIndex(),
                        chunk.getText(),
                        chunk.isActive()))
                .toList();
    }
}
