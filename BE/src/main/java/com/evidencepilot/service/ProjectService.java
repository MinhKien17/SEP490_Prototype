package com.evidencepilot.service;

import com.evidencepilot.dto.request.ProjectCreateRequest;
import com.evidencepilot.dto.request.ProjectUpdateRequest;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.enums.ProjectRole;
import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectResponse> getAllProjects();
    ProjectResponse getProjectById(UUID id);
    ProjectResponse createProject(ProjectCreateRequest request);
    ProjectResponse updateProject(UUID id, ProjectUpdateRequest request);
    void deleteProject(UUID id);
    List<ProjectMember> getProjectMembers(UUID projectId);
    void addMember(UUID projectId, UUID userId, ProjectRole role);
    void removeMember(UUID projectId, UUID userId);
}
