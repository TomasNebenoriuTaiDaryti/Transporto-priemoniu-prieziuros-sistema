package com.example.back.vehicle.dto;

public record VehicleDetailsResponse(
        Long id,
        String vin,
        String make,
        String model,
        Integer modelYear,
        Integer engineDisplacementCc,
        String fuelType,
        Long odometerKm,
        String transmission,
        String drive,
        String body,
        Integer doors,
        Integer seats,
        java.math.BigDecimal co2Gkm,
        String plantCountry,
        String manufacturer,
        Long groupId
) {}