package com.example.back.Unit.stats;

import com.example.back.stats.dto.VehicleStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleStatsResponseTest {

    @Test
    @DisplayName("VehicleStatsResponse saugo visus statistikos laukus")
    void vehicleStatsResponseStoresAllFields() {
        VehicleStatsResponse.MonthlyCostPoint monthlyCost = new VehicleStatsResponse.MonthlyCostPoint("2026-03", new BigDecimal("150.00"));
        VehicleStatsResponse.ExpenseByTypePoint expenseByType = new VehicleStatsResponse.ExpenseByTypePoint("Degalai", new BigDecimal("80.00"));
        VehicleStatsResponse.FuelHistoryPoint fuelHistory = new VehicleStatsResponse.FuelHistoryPoint("2026-03-10", new BigDecimal("25.00"), new BigDecimal("50.00"), new BigDecimal("2.00"));
        VehicleStatsResponse.MonthlyFuelAveragePoint monthlyFuelAverage = new VehicleStatsResponse.MonthlyFuelAveragePoint("2026-03", new BigDecimal("2.000"));

        VehicleStatsResponse response = new VehicleStatsResponse(1L, "Volkswagen Golf (2000)", List.of(monthlyCost), List.of(expenseByType), List.of(fuelHistory), List.of(monthlyFuelAverage));

        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.vehicleName()).isEqualTo("Volkswagen Golf (2000)");
        assertThat(response.monthlyCosts()).containsExactly(monthlyCost);
        assertThat(response.expenseByType()).containsExactly(expenseByType);
        assertThat(response.fuelHistory()).containsExactly(fuelHistory);
        assertThat(response.monthlyFuelAverages()).containsExactly(monthlyFuelAverage);
    }

    @Test
    @DisplayName("MonthlyCostPoint saugo mėnesį ir sumą")
    void monthlyCostPointStoresMonthAndAmount() {
        VehicleStatsResponse.MonthlyCostPoint point = new VehicleStatsResponse.MonthlyCostPoint("2026-03", new BigDecimal("150.00"));

        assertThat(point.month()).isEqualTo("2026-03");
        assertThat(point.amount()).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("ExpenseByTypePoint saugo tipą ir sumą")
    void expenseByTypePointStoresTypeAndAmount() {
        VehicleStatsResponse.ExpenseByTypePoint point = new VehicleStatsResponse.ExpenseByTypePoint("Servisas", new BigDecimal("100.00"));

        assertThat(point.type()).isEqualTo("Servisas");
        assertThat(point.amount()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("FuelHistoryPoint saugo kuro istorijos duomenis")
    void fuelHistoryPointStoresFuelHistoryData() {
        VehicleStatsResponse.FuelHistoryPoint point = new VehicleStatsResponse.FuelHistoryPoint("2026-03-10", new BigDecimal("25.00"), new BigDecimal("50.00"), new BigDecimal("2.00"));

        assertThat(point.date()).isEqualTo("2026-03-10");
        assertThat(point.liters()).isEqualTo(new BigDecimal("25.00"));
        assertThat(point.cost()).isEqualTo(new BigDecimal("50.00"));
        assertThat(point.pricePerLiter()).isEqualTo(new BigDecimal("2.00"));
    }

    @Test
    @DisplayName("MonthlyFuelAveragePoint saugo mėnesį ir vidutinę litro kainą")
    void monthlyFuelAveragePointStoresMonthAndAveragePrice() {
        VehicleStatsResponse.MonthlyFuelAveragePoint point = new VehicleStatsResponse.MonthlyFuelAveragePoint("2026-03", new BigDecimal("2.000"));

        assertThat(point.month()).isEqualTo("2026-03");
        assertThat(point.avgPricePerLiter()).isEqualTo(new BigDecimal("2.000"));
    }
}