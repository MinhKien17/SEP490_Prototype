package com.evidencepilot.repository;

import com.evidencepilot.model.SourceReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceReferenceRepository extends JpaRepository<SourceReference, UUID> {

    List<SourceReference> findBySourceIdOrderByReferenceIndex(UUID sourceId);

    List<SourceReference> findBySourceProjectIdOrderBySourceIdAscReferenceIndexAsc(UUID projectId);

    List<SourceReference> findBySourceProjectIdAndSourceActiveTrueOrderBySourceIdAscReferenceIndexAsc(UUID projectId);
}
