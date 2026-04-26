package com.example.back.Unit.auth;

import com.example.back.auth.domain.Account;
import com.example.back.auth.dto.AuthResponse;
import com.example.back.auth.dto.LoginRequest;
import com.example.back.auth.dto.RegisterRequest;
import com.example.back.auth.repo.AccountRepo;
import com.example.back.auth.service.AuthService;
import com.example.back.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final AccountRepo users = mock(AccountRepo.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService service = new AuthService(users, passwordEncoder, jwtService);

    @Test
    @DisplayName("Register sukuria vartotoja ir grazina bearer token")
    void registerCreatesUserAndReturnsBearerToken() {
        RegisterRequest request = new RegisterRequest("USER@Test.COM", "password");

        when(users.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hash");
        when(users.save(any(Account.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));
        when(jwtService.generateAccessToken(10L, "user@test.com")).thenReturn("token");

        AuthResponse result = service.register(request);

        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.tokenType()).isEqualTo("Bearer");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@test.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");

        verify(jwtService).generateAccessToken(10L, "user@test.com");
    }

    @Test
    @DisplayName("Register meta klaida kai email jau naudojamas")
    void registerThrowsWhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("USER@Test.COM", "password");
        when(users.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("Email already used");

        verify(passwordEncoder, never()).encode(anyString());
        verify(users, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Login grazina bearer token kai duomenys teisingi")
    void loginReturnsBearerTokenWhenCredentialsAreValid() {
        Account account = withId(new Account("USER@Test.COM", "hash"), 10L);
        LoginRequest request = new LoginRequest("USER@Test.COM", "password");

        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(10L, "user@test.com")).thenReturn("token");

        AuthResponse result = service.login(request);

        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        verify(users).findByEmail("user@test.com");
        verify(passwordEncoder).matches("password", "hash");
        verify(jwtService).generateAccessToken(10L, "user@test.com");
    }

    @Test
    @DisplayName("Login meta klaida kai email nerastas")
    void loginThrowsWhenEmailNotFound() {
        LoginRequest request = new LoginRequest("USER@Test.COM", "password");
        when(users.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid credentials");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Login meta klaida kai password neteisingas")
    void loginThrowsWhenPasswordIsInvalid() {
        Account account = withId(new Account("USER@Test.COM", "hash"), 10L);
        LoginRequest request = new LoginRequest("USER@Test.COM", "wrong");

        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid credentials");

        verify(jwtService, never()).generateAccessToken(anyLong(), anyString());
    }

    private static Account withId(Account account, Long id) {
        try {
            Field field = Account.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
            return account;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}