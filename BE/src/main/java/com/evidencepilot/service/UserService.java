package com.evidencepilot.service;

import com.evidencepilot.model.User;
import java.util.UUID;

public interface UserService {
    User findById(UUID id);
    User findByEmail(String email);
}
