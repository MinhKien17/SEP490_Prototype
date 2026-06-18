package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.repository.DatasetRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.repository.UserRepository;
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
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Source file uploads and CRUD operations.
 * Uploaded files are stored under the configured upload directory
 * and the resulting path is persisted to the DB.
 *
 * Base path: /api/sources
 */
@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceRepository sourceRepository;
    private final ProjectRepository projectRepository;
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;

    /** Root directory where uploaded files are stored inside the container. */
    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    // ── Standard CRUD ──────────────────────────────────────────────────────────

    @GetMapping
    public List<Source> findAll() {
        return sourceRepository.findAll();
    }

    @GetMapping("/{id}")
    public Source findById(@PathVariable Integer id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Source not found: " + id));
    }

    @GetMapping("/by-project/{projectId}")
    public List<Source> findByProject(@PathVariable Integer projectId) {
        return sourceRepository.findByProjectId(projectId);
    }

    @GetMapping("/by-dataset/{datasetId}")
    public List<Source> findByDataset(@PathVariable Integer datasetId) {
        return sourceRepository.findByDatasetId(datasetId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!sourceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found: " + id);
        }
        sourceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── File upload ────────────────────────────────────────────────────────────

    /**
     * Uploads a source file for a project or dataset.
     *
     * <p>The file is stored at {@code <uploadDir>/sources/<uuid>_<originalName>}
     * and the path is saved as {@code file_url} in the DB.  No hashing or
     * deduplication is performed.</p>
     *
     * @param file        the multipart file
     * @param uploadedById ID of the user uploading the file (required)
     * @param projectId   optional – if present the source is linked to this project
     * @param datasetId   optional – if present the source is linked to this dataset
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Source> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") Integer uploadedById,
            @RequestParam(value = "projectId", required = false) Integer projectId,
            @RequestParam(value = "datasetId", required = false) Integer datasetId) {

        // ── Resolve relations ──────────────────────────────────────────────────
        User uploader = userRepository.findById(uploadedById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + uploadedById));

        Source source = new Source();
        source.setUploadedBy(uploader);

        if (projectId != null) {
            source.setProject(projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Project not found: " + projectId)));
        }
        if (datasetId != null) {
            source.setDataset(datasetRepository.findById(datasetId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Dataset not found: " + datasetId)));
        }

        // ── Persist the file ───────────────────────────────────────────────────
        String savedPath = storeFile(file, "sources");
        source.setFileUrl(savedPath);

        Source saved = sourceRepository.save(source);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /**
     * Writes the multipart file to {@code <uploadDir>/<subDir>/<uuid>_<original>}
     * and returns the resulting absolute path string.
     */
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
