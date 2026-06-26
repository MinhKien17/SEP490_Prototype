package com.evidencepilot.repository;

import com.evidencepilot.model.DocumentText;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DocumentTextRepository extends JpaRepository<DocumentText, UUID> {
    DocumentText findByDocumentId(UUID documentId);
}
