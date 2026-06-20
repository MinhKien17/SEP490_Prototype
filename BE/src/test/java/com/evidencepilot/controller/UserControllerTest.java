package com.evidencepilot.controller;

import com.evidencepilot.config.JwtUtil;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.RegisterRequest;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtUtil jwtUtil = new JwtUtil("EvidencePilot-Test-Secret-Key-For-Jwt!!", 86_400_000L);
    private final UserController controller =
            new UserController(userRepository, new BCryptPasswordEncoder(), jwtUtil);

    @Test
    void registerCreatesStudentAndDoesNotExposePasswordHash() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Passw0rd!");

        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7);
            assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
            assertThat(saved.getPasswordHash()).startsWith("$2");
            return saved;
        });

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().email()).isEqualTo("student@example.com");
        assertThat(response.getBody().role()).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void registerRejectsDuplicateEmailBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Passw0rd!");

        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginTokenCarriesActualUserRole() {
        User user = new User();
        user.setId(7);
        user.setEmail("instructor@example.com");
        user.setRole(UserRole.INSTRUCTOR);
        user.setPasswordHash(new BCryptPasswordEncoder().encode("Passw0rd!"));

        when(userRepository.findByEmail("instructor@example.com")).thenReturn(Optional.of(user));

        var request = new com.evidencepilot.dto.LoginRequest();
        request.setEmail("instructor@example.com");
        request.setPassword("Passw0rd!");

        var response = controller.login(request);

        assertThat(jwtUtil.extractRole((String) response.getBody().get("token")))
                .isEqualTo(UserRole.INSTRUCTOR.name());
    }
}
