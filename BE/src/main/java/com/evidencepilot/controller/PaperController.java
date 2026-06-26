package com.evidencepilot.controller;

import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.DocumentService;
import com.evidencepilot.service.PaperProcessingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final DocumentRepository documentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final PaperProcessingService paperProcessingService;
    private final DocumentService documentService;

    @GetMapping
    public List<DocumentResponse> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        List<Document> docs;
        if (currentUserService.isAdmin(currentUser)) {
            docs = documentRepository.findAll().stream()
                    .filter(Document::isActive)
                    .toList();
        } else {
            docs = documentRepository.findAll().stream()
                    .filter(d -> d.isActive() && d.getProject() != null
                            && d.getProject().getStudent() != null
                            && d.getProject().getStudent().getId().equals(currentUser.getId()))
                    .toList();
        }
        return docs.stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse findById(@PathVariable UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!doc.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        if (doc.getProject() != null) {
            currentUserService.requireProjectWriteAccess(currentUser, doc.getProject());
        }
        return DocumentResponse.from(doc);
    }

    @GetMapping("/by-project/{projectId}")
    public List<DocumentResponse> findByProject(@PathVariable UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);
        return documentRepository.findByProjectId(projectId).stream()
                .filter(Document::isActive)
                .map(DocumentResponse::from)
                .toList();
    }

    @GetMapping("/{id}/sections")
    public List<PaperSectionResponse> sections(@PathVariable UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!doc.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        currentUserService.requireProjectAccess(currentUser, doc.getProject());
        return paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(id).stream()
                .map(s -> new PaperSectionResponse(
                        s.getId(), s.getDocument().getId(),
                        s.getAssignedUser() != null ? s.getAssignedUser().getId() : null,
                        s.getSectionOrder(), s.getSectionTitle(),
                        s.getContentTex(), s.getContentMdCache(), s.getUpdatedAt()))
                .toList();
    }

    @PostMapping("/{id}/review")
    public Map<String, Object> review(
            @PathVariable UUID id,
            @RequestParam(required = false) String targetStyle) {

        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        if (!doc.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        currentUserService.requireProjectAccess(currentUser, doc.getProject());
        return paperProcessingService.review(doc, targetStyle);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
        currentUserService.requireProjectAccess(currentUser, doc.getProject());
        doc.setActive(false);
        documentRepository.save(doc);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") UUID projectId) {

        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectAccess(currentUser, project);

        DocumentResponse response = documentService.uploadDocument(projectId, file, DocumentType.STUDENT_SUBMISSION);

        Document saved = documentRepository.findById(response.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Saved document not found immediately after upload"));
        paperProcessingService.detectAndPersistSections(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
