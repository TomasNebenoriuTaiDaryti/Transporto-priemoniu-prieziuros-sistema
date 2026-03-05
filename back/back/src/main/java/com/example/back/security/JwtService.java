package com.example.back.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
@Service
public class JwtService {

    private final String issuer;
    private final long ttlMinutes;
    private final byte[] secretBytes;

    public JwtService(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.accessTokenTtlMinutes}") long ttlMinutes,
            @Value("${app.jwt.secret}") String secret
    ) {
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secretBytes))
                .compact();
    }

    public Long parseUserId(String token) {
        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretBytes))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public String parseEmail(String token) {
        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretBytes))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object email = claims.get("email");
        return email == null ? null : email.toString();
    }
}