package com.example.back.Unit.security;

import com.example.back.security.JwtAuthFilter;
import com.example.back.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    @DisplayName("SecurityConfig sukuriamas su JWT filtru")
    void securityConfigIsCreatedWithJwtFilter() {
        JwtAuthFilter jwtAuthFilter = mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(jwtAuthFilter);
        assertThat(config).isNotNull();
    }
}