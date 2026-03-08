package com.example.back.vehicle.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VehicleConfirmRequest(
        @Size(min = 17, max = 17) String vin,
        @NotBlank String make,
        @NotBlank String model,
        @Min(1950) @Max(2100) Integer modelYear,
        @Min(300) @Max(10000) Integer engineDisplacementCc,
        String transmission,
        @NotNull @Min(0) Long odometerKm,
        @NotBlank String fuelType,
        String drive,
        String body,
        Integer doors,
        Integer seats,
        BigDecimal co2Gkm,
        String plantCountry,
        String manufacturer
) {}