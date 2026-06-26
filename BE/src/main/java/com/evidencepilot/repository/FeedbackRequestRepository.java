package com.evidencepilot.repository;

import com.evidencepilot.model.FeedbackRequest;
import com.evidencepilot.model.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, UUID> {

    List<FeedbackRequest> findByProjectId(UUID projectId);

    List<FeedbackRequest> findByStudentId(UUID studentId);

    List<FeedbackRequest> findByInstructorId(UUID instructorId);

    List<FeedbackRequest> findByStatus(FeedbackStatus status);

    boolean existsByProjectIdAndInstructorId(UUID projectId, UUID instructorId);
}
