package com.evidencepilot.service;

import com.evidencepilot.model.Source;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public interface SourceExtractionService {

    void extractAndPersist(Source source, MultipartFile file);

    ExtractedText extractText(MultipartFile file);

    record ExtractedText(String text, String method) {}
}
