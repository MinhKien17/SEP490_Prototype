package com.evidencepilot.dto.response;

import com.evidencepilot.model.User;
import com.evidencepilot.model.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Integer id,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
