package com.example.back.stats.dto;

import java.math.BigDecimal;
import java.util.List;

public record VehicleStatsResponse(
        Long vehicleId,
        String vehicleName,
        List<MonthlyCostPoint> monthlyCosts,
        List<ExpenseByTypePoint> expenseByType,
        List<FuelHistoryPoint> fuelHistory,
        List<MonthlyFuelAveragePoint> monthlyFuelAverages
) {
    public record MonthlyCostPoint(
            String month,
            BigDecimal amount
    ) {}

    public record ExpenseByTypePoint(
            String type,
            BigDecimal amount
    ) {}

    public record FuelHistoryPoint(
            String date,
            BigDecimal liters,
            BigDecimal cost,
            BigDecimal pricePerLiter
    ) {}

    public record MonthlyFuelAveragePoint(
            String month,
            BigDecimal avgPricePerLiter
    ) {}
}
