package com.evidencepilot.service;

import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.dto.request.RegisterRequest;
import com.evidencepilot.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    String verifyEmail(String token);
}
