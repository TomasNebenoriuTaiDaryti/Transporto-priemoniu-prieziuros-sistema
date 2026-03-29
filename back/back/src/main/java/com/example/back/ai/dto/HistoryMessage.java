package com.example.back.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record HistoryMessage(
        @NotBlank String role,
        @NotBlank String text
) {}