package com.evidencepilot.repository;

import com.evidencepilot.model.SectionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SectionFeedbackRepository extends JpaRepository<SectionFeedback, UUID> {
    List<SectionFeedback> findBySectionId(UUID sectionId);
    List<SectionFeedback> findByAuthorId(UUID authorId);
    List<SectionFeedback> findBySectionIdAndAuthorId(UUID sectionId, UUID authorId);
}
