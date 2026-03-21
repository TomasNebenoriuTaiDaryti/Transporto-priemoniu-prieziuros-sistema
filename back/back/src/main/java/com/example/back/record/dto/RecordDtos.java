package com.example.back.record.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public final class RecordDtos {
    private RecordDtos() {}

    public enum RecordKind { SERVICE, TIRES, BRAKES, OIL, OTHER, FUEL }

    public record RecordResponse(
            Long id,
            Long vehicleId,
            RecordKind kind,
            String title,
            String description,
            Instant performedAt,
            Long odometerKm,
            BigDecimal totalCost,
            String currency,
            String metaJson,
            Long createdByUserId,
            boolean canEdit
    ) {}

    public record CreateRecordRequest(
            @NotNull RecordKind kind,
            String title,
            String description,
            Instant performedAt,
            Long odometerKm,
            @NotNull @PositiveOrZero BigDecimal totalCost,
            @NotBlank String currency,
            String tireType,
            Integer tireAgeYears,
            BigDecimal liters
    ) {}

    public record UpdateRecordRequest(
            @NotNull RecordKind kind,
            String title,
            String description,
            @NotNull Instant performedAt,
            @NotNull @Min(0) Long odometerKm,
            @NotNull @PositiveOrZero BigDecimal totalCost,
            @NotBlank String currency,
            String tireType,
            Integer tireAgeYears,
            BigDecimal liters
    ) {}
}
