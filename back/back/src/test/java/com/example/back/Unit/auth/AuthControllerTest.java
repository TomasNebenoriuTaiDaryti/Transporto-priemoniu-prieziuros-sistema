package com.example.back.Unit.auth;

import com.example.back.auth.api.AuthController;
import com.example.back.auth.dto.AuthResponse;
import com.example.back.auth.dto.LoginRequest;
import com.example.back.auth.dto.RegisterRequest;
import com.example.back.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController controller = new AuthController(authService);

    @Test
    @DisplayName("Register perduoda request i servisa")
    void registerDelegatesToService() {
        RegisterRequest request = new RegisterRequest("USER@Test.COM", "password");
        AuthResponse response = AuthResponse.bearer("token");

        when(authService.register(request)).thenReturn(response);

        AuthResponse result = controller.register(request);

        assertThat(result).isEqualTo(response);
        verify(authService).register(request);
    }

    @Test
    @DisplayName("Login perduoda request i servisa")
    void loginDelegatesToService() {
        LoginRequest request = new LoginRequest("USER@Test.COM", "password");
        AuthResponse response = AuthResponse.bearer("token");

        when(authService.login(request)).thenReturn(response);

        AuthResponse result = controller.login(request);

        assertThat(result).isEqualTo(response);
        verify(authService).login(request);
    }
}