package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for Project CRUD operations.
 * Base path: /api/projects
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;

    @GetMapping
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @GetMapping("/{id}")
    public Project findById(@PathVariable Integer id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + id));
    }

    @GetMapping("/by-student/{studentId}")
    public List<Project> findByStudent(@PathVariable Integer studentId) {
        return projectRepository.findByStudentId(studentId);
    }

    @PostMapping
    public ResponseEntity<Project> create(@RequestBody Project project) {
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Integer id, @RequestBody Project project) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + id);
        }
        project.setId(id);
        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + id);
        }
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
