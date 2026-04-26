package com.example.back.Unit.security;

import com.example.back.security.AuthUser;
import com.example.back.security.JwtAuthFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthUserTest {

    @Test
    @DisplayName("Grzaina vartotojo ID is JWT principal")
    void userIdReturnsUserIdFromJwtPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new JwtAuthFilter.UserPrincipal(10L, "user@test.com"));

        Long result = AuthUser.userId(auth);

        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("Meta klaida, kai principal nera JWT vartotojas")
    void userIdThrowsWhenPrincipalIsNotJwtUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        assertThatThrownBy(() -> AuthUser.userId(auth)).isInstanceOf(IllegalStateException.class).hasMessage("No authenticated user");
    }

    @Test
    @DisplayName("Meta klaida, kai principal yra null")
    void userIdThrowsWhenPrincipalIsNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(null);

        assertThatThrownBy(() -> AuthUser.userId(auth)).isInstanceOf(IllegalStateException.class).hasMessage("No authenticated user");
    }
}