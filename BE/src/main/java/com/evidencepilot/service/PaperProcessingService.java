package com.evidencepilot.service;

import com.evidencepilot.model.Paper;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.dto.response.PaperReviewResponse;

import java.util.List;

public interface PaperProcessingService {

    List<PaperSection> detectAndPersistSections(Paper paper);

    PaperReviewResponse review(Paper paper, String targetStyle);
}
