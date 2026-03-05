package com.example.back.auth.service;

import com.example.back.auth.domain.Account;
import com.example.back.auth.dto.AuthResponse;
import com.example.back.auth.dto.LoginRequest;
import com.example.back.auth.dto.RegisterRequest;
import com.example.back.auth.repo.AccountRepo;
import com.example.back.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class AuthService {

    private final AccountRepo users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AccountRepo users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();

        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already used");
        }

        String hash = passwordEncoder.encode(req.password());
        Account user = users.save(new Account(email, hash));

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().toLowerCase();
        Account user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        boolean ok = passwordEncoder.matches(req.password(), user.getPasswordHash());
        if (!ok) throw new IllegalArgumentException("Invalid credentials");

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token);
    }
}
