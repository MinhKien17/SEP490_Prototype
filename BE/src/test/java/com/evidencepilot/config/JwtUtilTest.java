package com.evidencepilot.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            "EvidencePilot-Test-Secret-Key-For-Jwt!!",
            86_400_000L
    );

    @Test
    void generatedTokenIsValidAndContainsEmailSubject() {
        String token = jwtUtil.generateToken("student@example.com");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("student@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("STUDENT");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken("student@example.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }
}
