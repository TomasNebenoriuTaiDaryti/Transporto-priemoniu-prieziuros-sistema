package com.example.back.vehicle.dto;

public record VehicleListItemResponse(
        Long id,
        String vin,
        String make,
        String model,
        Integer modelYear,
        String fuelType,
        Long odometerKm,
        Long groupId,
        Long ownerUserId,
        String ownerEmail,
        boolean canEdit,
        boolean canDelete,
        boolean activeByMe,
        String activeByEmail
) {}
