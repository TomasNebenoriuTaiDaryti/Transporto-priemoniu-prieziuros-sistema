package com.example.back.vehicle.dto;

public record VehiclePreviewResponse(
        String vin,
        String make,
        String model,
        Integer modelYear,
        Integer engineDisplacementCc,
        String transmission,
        String fuelTypeRaw,
        String fuelType,
        String drive,
        String body,
        Integer doors,
        Integer seats,
        Double co2Gkm,
        String plantCountry,
        String manufacturer
) {}