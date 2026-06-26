package com.evidencepilot.service.impl;

import com.evidencepilot.infrastructure.AiModelClient;
import com.evidencepilot.infrastructure.AiModelClient.AiApiException;
import com.evidencepilot.infrastructure.AiModelClient.ExtractDocumentResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Source;
import com.evidencepilot.model.SourceChunk;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.DocumentTextRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.service.SourceExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourceExtractionServiceImpl implements SourceExtractionService {

    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final AiModelClient aiModelClient;

    @RabbitListener(queues = "extraction.queue")
    @Transactional
    public void handleExtraction(String documentIdStr) {
        UUID documentId = UUID.fromString(documentIdStr);
        log.info("Processing extraction for document {}", documentId);

        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Document {} not found for extraction", documentId);
            return;
        }

        doc.setProcessingStatus(ProcessingStatus.PROCESSING);
        documentRepository.save(doc);

        try {
            String extractedText;
            String extractionMethod;

            try {
                byte[] content = fetchContent(doc);
                ExtractDocumentResponse aiResponse = aiModelClient.extractDocument(
                        doc.getId(), doc.getOriginalFilename(), doc.getContentType(), content);
                extractedText = aiResponse.markdown();
                extractionMethod = "ai:" + aiResponse.method();
            } catch (AiApiException e) {
                log.warn("AI extraction failed for document {}, using fallback", documentId, e);
                extractedText = extractFallbackText(doc);
                extractionMethod = "fallback";
            }

            DocumentText docText = new DocumentText();
docText.setDocument(doc);
            docText.setExtractedText(extractedText);
            docText.setExtractionMethod(extractionMethod);
            documentTextRepository.save(docText);

            List<DocumentChunk> chunks = chunkText(doc, extractedText);
            documentChunkRepository.saveAll(chunks);

            doc.setProcessingStatus(ProcessingStatus.READY);
            doc.setProcessedAt(LocalDateTime.now());
            documentRepository.save(doc);

            log.info("Extraction completed for document {} ({} chunks)", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Extraction failed for document {}", documentId, e);
            doc.setProcessingStatus(ProcessingStatus.FAILED);
            doc.setProcessingError(e.getMessage());
            documentRepository.save(doc);
        }
    }

    private byte[] fetchContent(Document doc) {
        String text = "Extracted content from " +
                (doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "document");
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String extractFallbackText(Document doc) {
        return "Fallback extracted text for " +
                (doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "document " + doc.getId());
    }

    private List<DocumentChunk> chunkText(Document doc, String text) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        int maxChunkSize = 1000;
        int index = 0;
        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() > maxChunkSize && current.length() > 0) {
                DocumentChunk chunk = new DocumentChunk();
chunk.setDocument(doc);
                chunk.setChunkIndex(index++);
                chunk.setText(current.toString().trim());
                chunk.setActive(true);
                chunks.add(chunk);
                current = new StringBuilder();
            }
            current.append(paragraph).append("\n\n");
        }
        if (current.length() > 0) {
            DocumentChunk chunk = new DocumentChunk();
chunk.setDocument(doc);
            chunk.setChunkIndex(index);
            chunk.setText(current.toString().trim());
            chunk.setActive(true);
            chunks.add(chunk);
        }
        return chunks;
    }

    @Override
    @Transactional
    public void extractAndPersist(Source source, MultipartFile file) {
        ExtractedText extracted = extractText(file);
        source.setExtractedText(extracted.text());
        source.setExtractionMethod(extracted.method());

        try {
            byte[] raw = file.getBytes();
            ExtractDocumentResponse response = aiModelClient.extractDocument(
                    source.getId(), file.getOriginalFilename(), file.getContentType(), raw);

            String markdown = response.markdown();
            if (markdown != null && !markdown.isBlank()) {
                persistSourceChunks(source, markdown);
            }
        } catch (AiApiException | IOException e) {
            log.warn("AI extraction failed for source {}, using fallback: {}", source.getId(), e.getMessage());
            persistSourceChunks(source, extracted.text());
        }
    }

    @Override
    public ExtractedText extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String method = "unknown";
        String text;

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
        byte[] bytes = file.getBytes();
        String raw = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (!raw.isBlank() && raw.contains(" ")) {
            return raw;
        }
        return "PDF content could not be extracted (pdfbox not available).";
    }

    private void persistSourceChunks(Source source, String text) {
        String[] chunks = splitIntoChunks(text, 1000);
        List<SourceChunk> sourceChunks = new ArrayList<>();
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
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() > maxChunkSize && current.length() > 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(paragraph).append("\n\n");
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result.toArray(new String[0]);
    }
}
