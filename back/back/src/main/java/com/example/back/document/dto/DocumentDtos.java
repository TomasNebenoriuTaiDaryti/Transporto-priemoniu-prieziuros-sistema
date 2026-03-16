package com.example.back.document.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public final class DocumentDtos {
    private DocumentDtos() {}

    public enum DocumentType { INSURANCE, INSPECTION, FINE, OTHER }

    public record DocumentResponse(
            UUID id,
            Long vehicleId,
            DocumentType type,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String metaJson,
            Long createdByUserId,
            boolean canEdit
    ) {}

    public record CreateInsuranceRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull @PositiveOrZero Double price
    ) {}

    public record CreateInspectionRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record CreateFineRequest(
            @NotNull LocalDate date,
            @NotNull @Positive Double amount,
            @NotBlank String reason
    ) {}

    public record CreateOtherDocRequest(
            @NotBlank String title,
            String description,
            LocalDate endDate
    ) {}

    public record UpdateDocRequest(
            @NotBlank String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String metaJson
    ) {}
}