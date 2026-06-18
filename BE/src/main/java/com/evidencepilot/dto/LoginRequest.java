package com.evidencepilot.dto;

import lombok.Data;

/**
 * DTO for user login requests.
 */
@Data
public class LoginRequest {
    private String email;
    private String password;
}
