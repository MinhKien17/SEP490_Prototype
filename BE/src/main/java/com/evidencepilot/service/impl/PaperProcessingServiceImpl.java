package com.evidencepilot.service.impl;

import com.evidencepilot.client.ai.AiModelClient;
import com.evidencepilot.client.ai.AiModelClient.AiApiException;
import com.evidencepilot.dto.request.PaperReviewRequest;
import com.evidencepilot.dto.response.PaperReviewResponse;
import com.evidencepilot.model.Paper;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.repository.PaperRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.PaperProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperProcessingServiceImpl implements PaperProcessingService {

    private final AiModelClient aiModelClient;
    private final PaperRepository paperRepository;
    private final PaperSectionRepository paperSectionRepository;

    @Override
    @Transactional
    public List<PaperSection> detectAndPersistSections(Paper paper) {
        String text = paper.getExtractedText();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<PaperSection> sections = parseSections(text, paper);
        return paperSectionRepository.saveAll(sections);
    }

    @Override
    public PaperReviewResponse review(Paper paper, String targetStyle) {
        String paperId = "paper-" + paper.getId();
        PaperReviewRequest request = PaperReviewRequest.of(paperId, targetStyle, true);
        try {
            return aiModelClient.reviewPaper(request);
        } catch (AiApiException e) {
            log.error("Paper review failed for paper {}: {}", paper.getId(), e.getMessage());
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Paper review service unavailable", e);
        }
    }

    private List<PaperSection> parseSections(String text, Paper paper) {
        Pattern pattern = Pattern.compile("(?m)^(?:#{1,6}\\s+)?([A-Z][A-Za-z\\s]+)\\s*\\n");
        Matcher matcher = pattern.matcher(text);

        List<PaperSection> sections = new java.util.ArrayList<>();
        int index = 0;
        int lastEnd = 0;

        while (matcher.find()) {
            String sectionName = matcher.group(1).trim();
            int start = matcher.start();

            if (index > 0) {
                sections.get(index - 1).setText(text.substring(lastEnd, start).trim());
            }

            PaperSection section = new PaperSection();
            section.setPaper(paper);
            section.setSectionIndex(index);
            section.setName(sectionName);
            sections.add(section);

            lastEnd = matcher.end();
            index++;
        }

        if (!sections.isEmpty()) {
            sections.get(sections.size() - 1).setText(text.substring(lastEnd).trim());
        }

        if (sections.isEmpty()) {
            PaperSection section = new PaperSection();
            section.setPaper(paper);
            section.setSectionIndex(0);
            section.setName("Full Text");
            section.setText(text);
            sections.add(section);
        }

        return sections;
    }
}
