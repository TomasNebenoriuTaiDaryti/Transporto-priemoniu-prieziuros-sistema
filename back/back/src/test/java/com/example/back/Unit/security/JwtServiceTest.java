package com.example.back.Unit.security;

import com.example.back.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    @Test
    @DisplayName("Sugeneruoja tokena ir iš jo perskaito userId bei email")
    void generateAccessTokenAndParseUserIdAndEmail() {
        JwtService service = new JwtService("test-issuer", 60, SECRET);

        String token = service.generateAccessToken(10L, "user@test.com");

        assertThat(token).isNotBlank();
        assertThat(service.parseUserId(token)).isEqualTo(10L);
        assertThat(service.parseEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("Email gali buti null")
    void emailCanBeNull() {
        JwtService service = new JwtService("test-issuer", 60, SECRET);

        String token = service.generateAccessToken(10L, null);

        assertThat(service.parseUserId(token)).isEqualTo(10L);
        assertThat(service.parseEmail(token)).isNull();
    }

    @Test
    @DisplayName("Blogas tokenas meta klaida")
    void invalidTokenThrowsException() {
        JwtService service = new JwtService("test-issuer", 60, SECRET);

        assertThatThrownBy(() -> service.parseUserId("bad-token")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> service.parseEmail("bad-token")).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Tokenas su neteisinga paslaptimi neatidaromas")
    void tokenSignedWithDifferentSecretThrowsException() {
        JwtService first = new JwtService("test-issuer", 60, SECRET);
        JwtService second = new JwtService("test-issuer", 60, "abcdefghijklmnopqrstuvwxyz123456");

        String token = first.generateAccessToken(10L, "user@test.com");

        assertThatThrownBy(() -> second.parseUserId(token)).isInstanceOf(Exception.class);
    }
}