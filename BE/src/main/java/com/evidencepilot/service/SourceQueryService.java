package com.evidencepilot.service;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.model.User;
import java.util.List;
import java.util.UUID;

public interface SourceQueryService {
    List<DocumentTextResponse> getDocumentTextsByProject(UUID projectId);
    DocumentTextResponse getProjectDocumentText(UUID projectId, UUID documentId, User currentUser);
    List<DocumentChunkResponse> getDocumentChunks(UUID documentId);
}
