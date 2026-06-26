package com.evidencepilot.repository;

import com.evidencepilot.model.SourceText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceTextRepository extends JpaRepository<SourceText, UUID> {

    Optional<SourceText> findBySourceId(UUID sourceId);
}
