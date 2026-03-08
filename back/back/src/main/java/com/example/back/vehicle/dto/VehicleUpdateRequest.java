package com.example.back.vehicle.dto;

import jakarta.validation.constraints.*;

public record VehicleUpdateRequest(
        @NotBlank String make,
        @NotBlank String model,
        @NotNull Integer modelYear,
        @NotNull Integer engineDisplacementCc,
        @NotBlank
        @NotBlank String fuelType,
        @NotNull @Min(0) Long odometerKm,
        String transmission,
        String drive,
        String body,
        Integer doors,
        Integer seats,
        Double co2Gkm,
        String plantCountry,
        String manufacturer
) {}
