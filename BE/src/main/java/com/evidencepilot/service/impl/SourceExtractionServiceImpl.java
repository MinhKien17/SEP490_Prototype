package com.evidencepilot.service.impl;

import com.evidencepilot.client.ai.AiModelClient;
import com.evidencepilot.client.ai.AiModelClient.AiApiException;
import com.evidencepilot.client.ai.AiModelClient.ExtractDocumentResponse;
import com.evidencepilot.model.Source;
import com.evidencepilot.model.SourceChunk;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.service.SourceExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourceExtractionServiceImpl implements SourceExtractionService {

    private final AiModelClient aiModelClient;
    private final SourceChunkRepository sourceChunkRepository;

    @Override
    @Transactional
    public void extractAndPersist(Source source, MultipartFile file) {
        ExtractedText extracted = extractText(file);
        source.setExtractedText(extracted.text());
        source.setExtractionMethod(extracted.method());

        try {
            byte[] raw = file.getBytes();
            ExtractDocumentResponse response = aiModelClient.extractDocument(
                    file.getOriginalFilename(), file.getContentType(), raw);

            String markdown = response.markdown();
            if (markdown != null && !markdown.isBlank()) {
                persistChunks(source, markdown);
            }
        } catch (AiApiException | IOException e) {
            log.warn("AI extraction failed for source {}, using fallback: {}", source.getId(), e.getMessage());
            persistChunks(source, extracted.text());
        }
    }

    @Override
    public ExtractedText extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String method = "unknown";
        String text = "";

        try {
            if ("application/pdf".equals(contentType) || (filename != null && filename.toLowerCase().endsWith(".pdf"))) {
                text = extractPdfText(file);
                method = "pdfbox";
            } else {
                text = new String(file.getBytes());
                method = "raw";
            }
        } catch (IOException e) {
            log.warn("Text extraction failed for file {}: {}", filename, e.getMessage());
            text = "";
            method = "failed";
        }

        return new ExtractedText(text, method);
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (var inputStream = file.getInputStream()) {
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(inputStream);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private void persistChunks(Source source, String text) {
        String[] chunks = splitIntoChunks(text, 1000);
        List<SourceChunk> sourceChunks = new java.util.ArrayList<>();

        for (int i = 0; i < chunks.length; i++) {
            SourceChunk chunk = new SourceChunk();
            chunk.setSource(source);
            chunk.setChunkIndex(i);
            chunk.setText(chunks[i]);
            sourceChunks.add(chunk);
        }

        sourceChunkRepository.saveAll(sourceChunks);
    }

    private String[] splitIntoChunks(String text, int maxChunkSize) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }

        List<String> chunks = new java.util.ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks.toArray(new String[0]);
    }
}
