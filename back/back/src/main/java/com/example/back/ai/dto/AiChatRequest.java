package com.example.back.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiChatRequest(
        @NotNull Long vehicleId,
        @NotBlank String message,
        List<HistoryMessage> history
) {}
