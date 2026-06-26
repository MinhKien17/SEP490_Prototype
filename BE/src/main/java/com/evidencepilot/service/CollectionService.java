package com.evidencepilot.service;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.dto.response.CollectionResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionService {

    CollectionResponse createCollection(CollectionRequest request);

    CollectionResponse getCollectionById(UUID id);

    List<CollectionResponse> getCollectionsByProjectId(UUID projectId);

    void deleteCollection(UUID id);
}
