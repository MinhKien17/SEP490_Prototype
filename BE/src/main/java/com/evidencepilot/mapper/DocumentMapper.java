package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class DocumentMapper {

    public DocumentResponse toDocumentResponse(Document entity) {
        if (entity == null) return null;
        return DocumentResponse.from(entity);
    }

    public DocumentTextResponse toDocumentTextResponse(DocumentText entity) {
        if (entity == null) return null;
        UUID documentId = entity.getDocument() != null ? entity.getDocument().getId() : null;
        return new DocumentTextResponse(
                entity.getId(),
                documentId,
                entity.getExtractedText(),
                entity.getExtractionMethod());
    }

    public DocumentChunkResponse toDocumentChunkResponse(DocumentChunk entity) {
        if (entity == null) return null;
        UUID documentId = entity.getDocument() != null ? entity.getDocument().getId() : null;
        return new DocumentChunkResponse(
                entity.getId(),
                documentId,
                entity.getChunkIndex(),
                entity.getText(),
                entity.isActive());
    }
}
