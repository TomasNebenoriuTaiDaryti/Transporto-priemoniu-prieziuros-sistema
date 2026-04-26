package com.example.back.Unit.security;

import com.example.back.security.PasswordConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConfigTest {

    @Test
    @DisplayName("Sukuria BCrypt password encoder")
    void passwordEncoderCreatesBCryptEncoder() {
        PasswordConfig config = new PasswordConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isNotNull();
        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
        assertThat(encoder.matches("wrong", encoder.encode("secret"))).isFalse();
    }
}