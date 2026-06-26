package com.evidencepilot.repository;

import com.evidencepilot.model.DocumentReference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DocumentReferenceRepository extends JpaRepository<DocumentReference, UUID> {
}
