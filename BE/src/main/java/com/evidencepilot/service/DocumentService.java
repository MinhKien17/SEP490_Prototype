package com.evidencepilot.service;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.model.enums.DocumentType;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentResponse getDocumentById(UUID id);
    List<DocumentResponse> getDocumentsByProject(UUID projectId);
    DocumentResponse uploadDocument(UUID projectId, MultipartFile file, DocumentType docType);
    List<DocumentChunkResponse> getDocumentChunks(UUID documentId);
    DocumentTextResponse getDocumentText(UUID documentId);
    void deleteDocument(UUID id);
}
