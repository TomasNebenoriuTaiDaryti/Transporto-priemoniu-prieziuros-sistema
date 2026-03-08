package com.example.back.vehicle.dto;
public record VehicleResponse(
        Long id,
        String vin,
        String make,
        String model,
        Integer modelYear,
        String fuelType,
        Long odometerKm,
        Long groupId
) {}
