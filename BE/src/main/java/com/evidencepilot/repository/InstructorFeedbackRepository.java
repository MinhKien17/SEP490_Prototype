package com.evidencepilot.repository;

import com.evidencepilot.model.InstructorFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstructorFeedbackRepository extends JpaRepository<InstructorFeedback, Integer> {

    Optional<InstructorFeedback> findByRequestId(Integer requestId);

    java.util.List<InstructorFeedback> findByInstructorId(Integer instructorId);
}
