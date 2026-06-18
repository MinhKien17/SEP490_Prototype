package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Paper;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.repository.PaperRepository;
import com.evidencepilot.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Paper file uploads and CRUD operations.
 * Uploaded files are stored under the configured upload directory
 * and the resulting path is persisted to the DB.
 *
 * Base path: /api/papers
 */
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperRepository paperRepository;
    private final ProjectRepository projectRepository;

    /** Root directory where uploaded files are stored inside the container. */
    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    // ── Standard CRUD ──────────────────────────────────────────────────────────

    @GetMapping
    public List<Paper> findAll() {
        return paperRepository.findAll();
    }

    @GetMapping("/{id}")
    public Paper findById(@PathVariable Integer id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper not found: " + id));
    }

    @GetMapping("/by-project/{projectId}")
    public List<Paper> findByProject(@PathVariable Integer projectId) {
        return paperRepository.findByProjectId(projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!paperRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + id);
        }
        paperRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── File upload ────────────────────────────────────────────────────────────

    /**
     * Uploads a paper (PDF/document) and links it to a project.
     *
     * <p>The file is stored at {@code <uploadDir>/papers/<uuid>_<originalName>}
     * and the path is saved as {@code file_url} in the DB.  No hashing or
     * deduplication is performed.</p>
     *
     * @param file      the multipart file
     * @param projectId the project this paper belongs to (required)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Paper> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Integer projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));

        String savedPath = storeFile(file, "papers");

        Paper paper = new Paper();
        paper.setProject(project);
        paper.setFileUrl(savedPath);
        paper.setSubmittedAt(LocalDateTime.now());

        Paper saved = paperRepository.save(paper);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private String storeFile(MultipartFile file, String subDir) {
        try {
            Path directory = Paths.get(uploadDir, subDir);
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destination = directory.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return destination.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage(), e);
        }
    }
}
