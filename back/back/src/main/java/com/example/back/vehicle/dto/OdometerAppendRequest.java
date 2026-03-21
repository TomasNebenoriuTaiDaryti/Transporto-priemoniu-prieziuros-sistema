package com.example.back.vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OdometerAppendRequest(
        @NotNull @Min(0) Long deltaKm
) {}
