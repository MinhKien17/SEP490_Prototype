package com.evidencepilot.repository;

import com.evidencepilot.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {

    List<Source> findByProjectId(UUID projectId);

    List<Source> findByCollectionId(UUID collectionId);

    List<Source> findByUploadedById(UUID userId);

    List<Source> findByCollectionInstructorId(UUID instructorId);

    List<Source> findByActiveTrue();

    List<Source> findByProjectIdAndActiveTrue(UUID projectId);

    List<Source> findByCollectionIdAndActiveTrue(UUID collectionId);

    List<Source> findByCollectionInstructorIdAndActiveTrue(UUID instructorId);

    Optional<Source> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<Source> findByIdAndProjectIdAndActiveTrue(UUID id, UUID projectId);
}
