package com.evidencepilot.service;

import com.evidencepilot.model.User;
import com.evidencepilot.dto.response.SourceResponseDto;
import java.util.List;

public interface SourceQueryService {
    List<SourceResponseDto> getSourcesByProject(Integer projectId);
    SourceResponseDto getProjectSource(Integer projectId, Integer sourceId, User currentUser);
}
