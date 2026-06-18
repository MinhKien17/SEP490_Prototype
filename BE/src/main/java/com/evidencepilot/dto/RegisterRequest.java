package com.evidencepilot.dto;

import com.evidencepilot.domain.enums.UserRole;
import lombok.Data;

/**
 * DTO for user registration requests.
 */
@Data
public class RegisterRequest {
    private String email;
    private String password;
    private UserRole role;
}
