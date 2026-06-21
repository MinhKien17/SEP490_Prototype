package com.evidencepilot.controller;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.SourceChunk;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.response.DatasetGraphResponseDto;
import com.evidencepilot.dto.response.DatasetResponseDto;
import com.evidencepilot.dto.response.DatasetSimilarityResponseDto;
import com.evidencepilot.dto.response.DatasetSourceUploadResponseDto;
import com.evidencepilot.dto.response.SourceChunkResponseDto;
import com.evidencepilot.dto.response.SourceResponseDto;
import com.evidencepilot.repository.DatasetRepository;
import com.evidencepilot.repository.SourceChunkRepository;
import com.evidencepilot.repository.SourceRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.QdrantClient;
import com.evidencepilot.service.QdrantSearchResult;
import com.evidencepilot.service.SourceExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST controller for instructor datasets and dataset-local source exploration.
 * Base path: /api/datasets
 */
@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@Tag(name = "Datasets", description = "Instructor dataset CRUD, source upload, chunk browsing, semantic search, and graph data")
public class DatasetController {

    private final DatasetRepository datasetRepository;
    private final CurrentUserService currentUserService;
    private final SourceRepository sourceRepository;
    private final SourceChunkRepository sourceChunkRepository;
    private final UserRepository userRepository;
    private final SourceExtractionService sourceExtractionService;
    private final AiModelClient aiModelClient;
    private final QdrantClient qdrantClient;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir = "/app/uploads";

    @Operation(
            summary = "List accessible datasets",
            description = "Returns active datasets visible to the authenticated caller. "
                    + "Admins receive all active datasets, instructors receive their own datasets, "
                    + "and students receive an empty list. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset list returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<DatasetResponseDto> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        List<Dataset> datasets;
        if (currentUserService.isAdmin(currentUser)) {
            datasets = datasetRepository.findByActiveTrue();
        } else if (currentUserService.isInstructor(currentUser)) {
            datasets = datasetRepository.findByInstructorIdAndActiveTrue(currentUser.getId());
        } else {
            datasets = List.of();
        }
        return datasets.stream().map(DatasetResponseDto::fromEntity).toList();
    }

    @Operation(
            summary = "Get dataset by ID",
            description = "Returns a single active dataset by ID after enforcing dataset ownership. "
                    + "Only the owning instructor or an admin can access the dataset. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive")
    })
    @GetMapping("/{id}")
    public DatasetResponseDto findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, dataset);
        return DatasetResponseDto.fromEntity(dataset);
    }

    @Operation(
            summary = "List datasets by instructor",
            description = "Returns active datasets owned by the specified instructor. "
                    + "The caller must be that instructor or an admin. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructor dataset list returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - instructor ID access denied")
    })
    @GetMapping("/by-instructor/{instructorId}")
    public List<DatasetResponseDto> findByInstructor(@PathVariable Integer instructorId) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireUserIdOrAdmin(currentUser, instructorId);
        return datasetRepository.findByInstructorIdAndActiveTrue(instructorId).stream()
                .map(DatasetResponseDto::fromEntity)
                .toList();
    }

    @Operation(
            summary = "Create dataset",
            description = "Creates a dataset for the authenticated instructor. "
                    + "Non-admin instructors are always assigned as the dataset owner server-side. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dataset created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid dataset payload"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - caller is not an instructor or admin")
    })
    @PostMapping
    public ResponseEntity<DatasetResponseDto> create(@RequestBody Dataset dataset) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireRole(currentUser, UserRole.INSTRUCTOR);
        if (!currentUserService.isAdmin(currentUser)) {
            dataset.setInstructor(currentUser);
        }
        Dataset saved = datasetRepository.save(dataset);
        return ResponseEntity.status(HttpStatus.CREATED).body(DatasetResponseDto.fromEntity(saved));
    }

    @Operation(
            summary = "Update dataset",
            description = "Updates an existing active dataset after enforcing dataset ownership. "
                    + "Non-admin instructors cannot transfer ownership to another instructor. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid dataset payload"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive")
    })
    @PutMapping("/{id}")
    public DatasetResponseDto update(@PathVariable Integer id, @RequestBody Dataset dataset) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset existing = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, existing);
        dataset.setId(id);
        if (!currentUserService.isAdmin(currentUser)) {
            dataset.setInstructor(existing.getInstructor());
        }
        Dataset saved = datasetRepository.save(dataset);
        return DatasetResponseDto.fromEntity(saved);
    }

    @Operation(
            summary = "Soft-delete dataset",
            description = "Soft-deletes a dataset by setting active=false. "
                    + "The record is preserved but excluded from active dataset reads. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dataset soft-deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset existing = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, existing);
        existing.setActive(false);
        datasetRepository.save(existing);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Upload dataset source",
            description = "Uploads a source file into an instructor dataset. "
                    + "The backend stores the file, creates a Source linked to the dataset, extracts text, "
                    + "chunks the content, generates embeddings, and indexes vectors under DATASET scope. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dataset source uploaded and extracted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid multipart request or unsafe filename"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive"),
            @ApiResponse(responseCode = "422", description = "Uploaded file could not be extracted")
    })
    @PostMapping(value = "/{id}/sources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<DatasetSourceUploadResponseDto> uploadSource(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {

        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, dataset);

        User uploader = userRepository.findById(currentUser.getId()).orElse(currentUser);
        Source source = new Source();
        source.setDataset(dataset);
        source.setUploadedBy(uploader);
        source.setOriginalFilename(safeOriginalFilename(file));
        source.setContentType(file.getContentType());
        source.setFileSizeBytes(file.getSize());
        source.setFileUrl(storeFile(file, "sources"));

        Source saved = sourceRepository.save(source);
        sourceExtractionService.extractAndPersist(saved, file);

        long chunkCount = sourceChunkRepository.countBySourceId(saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new DatasetSourceUploadResponseDto(
                dataset.getId(),
                saved.getId(),
                saved.getOriginalFilename(),
                chunkCount,
                "EXTRACTED"
        ));
    }

    @Operation(
            summary = "List dataset sources",
            description = "Returns active source metadata for a single dataset after enforcing dataset access. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset source list returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive")
    })
    @GetMapping("/{id}/sources")
    public List<SourceResponseDto> sources(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, dataset);
        return sourceRepository.findByDatasetIdAndActiveTrue(id).stream()
                .map(SourceResponseDto::fromEntity)
                .toList();
    }

    @Operation(
            summary = "List dataset chunks (WIP)",
            description = "Returns extracted chunks for all sources in the dataset. "
                    + "This endpoint is intended for instructor review and graph preparation. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset chunks returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive")
    })
    @GetMapping("/{id}/chunks")
    public List<SourceChunkResponseDto> chunks(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, dataset);
        return sourceChunkRepository
                .findBySourceDatasetId(id)
                .stream()
                .map(SourceChunkResponseDto::fromEntity)
                .toList();
    }

    @Operation(
            summary = "Search similar dataset chunks (WIP)",
            description = "Embeds the query text and searches Qdrant for similar chunks inside this dataset's DATASET scope. "
                    + "The response includes matched chunks and their Qdrant similarity scores. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Similarity matches returned successfully"),
            @ApiResponse(responseCode = "400", description = "Query is blank or invalid"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive"),
            @ApiResponse(responseCode = "502", description = "Embedding service or Qdrant search failed")
    })
    @GetMapping("/{id}/similar")
    public DatasetSimilarityResponseDto similar(
            @PathVariable Integer id,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer topK) {

        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, dataset);
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query is required.");
        }

        int safeTopK = Math.max(1, Math.min(topK == null ? 5 : topK, 20));
        List<Float> queryVector = aiModelClient.generateEmbedding(query.trim());
        List<QdrantSearchResult> results = qdrantClient.findClosestChunks(
                queryVector,
                "DATASET",
                String.valueOf(dataset.getId()),
                safeTopK
        );

        List<Integer> chunkIds = results.stream()
                .map(result -> parseInteger(result.chunkId()))
                .filter(idValue -> idValue != null)
                .toList();
        Map<Integer, SourceChunk> chunksById = sourceChunkRepository.findAllById(chunkIds).stream()
                .collect(Collectors.toMap(SourceChunk::getId, Function.identity()));

        List<DatasetSimilarityResponseDto.Match> matches = new ArrayList<>();
        for (QdrantSearchResult result : results) {
            Integer chunkId = parseInteger(result.chunkId());
            if (chunkId == null) {
                continue;
            }
            SourceChunk chunk = chunksById.get(chunkId);
            if (chunk == null || chunk.getSource() == null || chunk.getSource().getDataset() == null
                    || !dataset.getId().equals(chunk.getSource().getDataset().getId())) {
                continue;
            }
            matches.add(new DatasetSimilarityResponseDto.Match(
                    SourceChunkResponseDto.fromEntity(chunk),
                    result.score()
            ));
        }

        return new DatasetSimilarityResponseDto(dataset.getId(), query.trim(), matches);
    }

    @Operation(
            summary = "Get dataset graph data (WIP)",
            description = "Builds graph-ready data for the dataset using source nodes, chunk nodes, and contains edges. "
                    + "Similarity edges can be layered by clients using the similarity endpoint. "
                    + "**Security:** Requires JWT Bearer Token. **Roles Allowed:** INSTRUCTOR, ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset graph data returned successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - dataset access denied"),
            @ApiResponse(responseCode = "404", description = "Dataset not found or inactive")
    })
    @GetMapping("/{id}/graph")
    public DatasetGraphResponseDto graph(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = requireActiveDataset(id);
        currentUserService.requireDatasetAccess(currentUser, dataset);

        List<Source> sources = sourceRepository.findByDatasetIdAndActiveTrue(id);
        List<SourceChunk> chunks = sourceChunkRepository
                .findBySourceDatasetId(id);

        List<DatasetGraphResponseDto.Node> nodes = new ArrayList<>();
        for (Source source : sources) {
            nodes.add(new DatasetGraphResponseDto.Node(
                    "source-" + source.getId(),
                    "source",
                    sourceLabel(source),
                    Map.of(
                            "sourceId", source.getId(),
                            "contentType", nullToBlank(source.getContentType()),
                            "fileSizeBytes", source.getFileSizeBytes() == null ? 0L : source.getFileSizeBytes()
                    )
            ));
        }

        List<DatasetGraphResponseDto.Edge> edges = new ArrayList<>();
        for (SourceChunk chunk : chunks) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("chunkId", chunk.getId());
            data.put("sourceId", chunk.getSource().getId());
            data.put("chunkIndex", chunk.getChunkIndex());
            data.put("text", chunk.getText());
            nodes.add(new DatasetGraphResponseDto.Node(
                    "chunk-" + chunk.getId(),
                    "chunk",
                    "Chunk " + chunk.getChunkIndex(),
                    data
            ));
            edges.add(new DatasetGraphResponseDto.Edge(
                    "source-" + chunk.getSource().getId(),
                    "chunk-" + chunk.getId(),
                    "contains",
                    null
            ));
        }

        return new DatasetGraphResponseDto(dataset.getId(), nodes, edges);
    }

    private Dataset requireActiveDataset(Integer id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        if (!dataset.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found: " + id);
        }
        return dataset;
    }

    private String storeFile(MultipartFile file, String subDir) {
        try {
            Path directory = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + "_" + safeOriginalFilename(file);
            Path destination = directory.resolve(filename).normalize();
            if (!destination.startsWith(directory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path detected");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage(), e);
        }
    }

    private String safeOriginalFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "uploaded-source";
        }
        String filename = Paths.get(original).getFileName().toString();
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sourceLabel(Source source) {
        if (source.getOriginalFilename() != null && !source.getOriginalFilename().isBlank()) {
            return source.getOriginalFilename();
        }
        return source.getFileUrl();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
