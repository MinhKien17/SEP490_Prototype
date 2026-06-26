package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import java.util.List;
import java.util.Map;

public interface PaperProcessingService {

    List<PaperSection> detectAndPersistSections(Document document);

    Map<String, Object> review(Document document, String targetStyle);
}
