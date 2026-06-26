package com.evidencepilot.repository;

import com.evidencepilot.model.SourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceChunkRepository extends JpaRepository<SourceChunk, UUID> {

    List<SourceChunk> findBySourceId(UUID sourceId);

    List<SourceChunk> findBySourceProjectId(UUID projectId);

    List<SourceChunk> findBySourceCollectionId(UUID collectionId);

    long countBySourceId(UUID sourceId);
}
