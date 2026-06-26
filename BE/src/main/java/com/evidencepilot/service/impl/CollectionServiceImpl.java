package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.dto.response.CollectionResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CollectionService;
import com.evidencepilot.service.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public CollectionResponse createCollection(CollectionRequest request) {
        User currentUser = currentUserService.requireCurrentUser();

        Project project = null;
        if (request.projectId() != null) {
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException(request.projectId(), "Project"));
        }

        Collection collection = new Collection();
collection.setTitle(request.name());
        collection.setDescription(request.description());
        collection.setProject(project);
        collection.setInstructor(currentUser);
        collection.setActive(true);
        collection.setCreatedAt(LocalDateTime.now());

        Collection saved = collectionRepository.save(collection);
        return toResponse(saved);
    }

    @Override
    public CollectionResponse getCollectionById(UUID id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Collection"));
        return toResponse(collection);
    }

    @Override
    public List<CollectionResponse> getCollectionsByProjectId(UUID projectId) {
        return collectionRepository.findByProjectId(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCollection(UUID id) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Collection"));
        collection.setActive(false);
        collectionRepository.save(collection);
    }

    private CollectionResponse toResponse(Collection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getTitle(),
                collection.getDescription(),
                collection.getProject() != null ? collection.getProject().getId() : null,
                collection.getCreatedAt());
    }
}
