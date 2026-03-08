package com.example.back.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehiclePreviewRequest(
        @NotBlank @Size(min = 17, max = 17) String vin
) {}