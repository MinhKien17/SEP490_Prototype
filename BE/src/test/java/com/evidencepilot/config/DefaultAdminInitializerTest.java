package com.evidencepilot.config;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAdminInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void missingAdminConfigDoesNothing() throws Exception {
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "", "");

        initializer.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
    }

    @Test
    void configuredAdminIsCreatedWhenMissing() throws Exception {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "admin@example.com", "AdminPass123!");

        initializer.run(new DefaultApplicationArguments());

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getEmail().equals("admin@example.com")
                        && user.getRole() == UserRole.ADMIN
                        && passwordEncoder.matches("AdminPass123!", user.getPasswordHash())
        ));
    }

    @Test
    void configuredAdminPasswordIsUpdatedWhenAdminExists() throws Exception {
        User existing = new User();
        existing.setEmail("admin@example.com");
        existing.setRole(UserRole.ADMIN);
        existing.setPasswordHash(passwordEncoder.encode("old-password"));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "admin@example.com", "AdminPass123!");

        initializer.run(new DefaultApplicationArguments());

        assertThat(passwordEncoder.matches("AdminPass123!", existing.getPasswordHash())).isTrue();
        verify(userRepository).save(existing);
    }
}
