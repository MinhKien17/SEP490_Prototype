package com.evidencepilot.repository;

import com.evidencepilot.model.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaperRepository extends JpaRepository<Paper, UUID> {

    List<Paper> findByProjectId(UUID projectId);

    List<Paper> findByActiveTrue();

    List<Paper> findByProjectIdAndActiveTrue(UUID projectId);
}
