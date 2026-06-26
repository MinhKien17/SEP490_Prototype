package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.PaperSectionResponse;
import com.evidencepilot.dto.response.ProjectMemberResponse;
import com.evidencepilot.dto.response.ProjectResponse;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ProjectMapper {

    public ProjectResponse toProjectResponse(Project entity) {
        if (entity == null) return null;
        return ProjectResponse.from(entity);
    }

    public ProjectMemberResponse toProjectMemberResponse(ProjectMember entity) {
        if (entity == null) return null;
        UUID projectId = entity.getProject() != null ? entity.getProject().getId() : null;
        UUID userId = entity.getUser() != null ? entity.getUser().getId() : null;
        String role = entity.getRole() != null ? entity.getRole().name() : null;
        return new ProjectMemberResponse(
                entity.getId(),
                projectId,
                userId,
                role,
                entity.getJoinedAt());
    }

    public PaperSectionResponse toPaperSectionResponse(PaperSection entity) {
        if (entity == null) return null;
        UUID documentId = entity.getDocument() != null ? entity.getDocument().getId() : null;
        UUID assignedUserId = entity.getAssignedUser() != null ? entity.getAssignedUser().getId() : null;
        return new PaperSectionResponse(
                entity.getId(),
                documentId,
                assignedUserId,
                entity.getSectionOrder(),
                entity.getSectionTitle(),
                entity.getContentTex(),
                entity.getContentMdCache(),
                entity.getUpdatedAt());
    }
}
