package com.example.back.Unit.auth;

import com.example.back.auth.dto.AuthResponse;
import com.example.back.auth.dto.LoginRequest;
import com.example.back.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtosTest {

    @Test
    @DisplayName("AuthResponse bearer sukuria Bearer token atsakyma")
    void authResponseBearerCreatesBearerTokenResponse() {
        AuthResponse response = AuthResponse.bearer("abc-token");

        assertThat(response.accessToken()).isEqualTo("abc-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("AuthResponse issaugo laukus")
    void authResponseStoresFields() {
        AuthResponse response = new AuthResponse("token", "Custom");

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Custom");
    }

    @Test
    @DisplayName("LoginRequest issaugo email ir password")
    void loginRequestStoresEmailAndPassword() {
        LoginRequest request = new LoginRequest("user@test.com", "password");

        assertThat(request.email()).isEqualTo("user@test.com");
        assertThat(request.password()).isEqualTo("password");
    }

    @Test
    @DisplayName("RegisterRequest issaugo email ir password")
    void registerRequestStoresEmailAndPassword() {
        RegisterRequest request = new RegisterRequest("user@test.com", "password");

        assertThat(request.email()).isEqualTo("user@test.com");
        assertThat(request.password()).isEqualTo("password");
    }
}