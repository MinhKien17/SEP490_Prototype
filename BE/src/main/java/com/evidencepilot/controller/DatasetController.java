package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.repository.DatasetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for Dataset CRUD operations.
 * Base path: /api/datasets
 */
@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetRepository datasetRepository;

    @GetMapping
    public List<Dataset> findAll() {
        return datasetRepository.findAll();
    }

    @GetMapping("/{id}")
    public Dataset findById(@PathVariable Integer id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
    }

    @GetMapping("/by-instructor/{instructorId}")
    public List<Dataset> findByInstructor(@PathVariable Integer instructorId) {
        return datasetRepository.findByInstructorId(instructorId);
    }

    @PostMapping
    public ResponseEntity<Dataset> create(@RequestBody Dataset dataset) {
        Dataset saved = datasetRepository.save(dataset);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Dataset update(@PathVariable Integer id, @RequestBody Dataset dataset) {
        if (!datasetRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found: " + id);
        }
        dataset.setId(id);
        return datasetRepository.save(dataset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!datasetRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found: " + id);
        }
        datasetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
