package com.example.back.Unit.stats;

import com.example.back.document.domain.VehicleDocument;
import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.record.domain.ServiceRecord;
import com.example.back.record.repo.ServiceRecordRepo;
import com.example.back.stats.dto.VehicleStatsResponse;
import com.example.back.stats.service.VehicleStatsService;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleStatsServiceTest {

    private final VehicleAccessService access = mock(VehicleAccessService.class);
    private final ServiceRecordRepo records = mock(ServiceRecordRepo.class);
    private final VehicleDocumentRepo documents = mock(VehicleDocumentRepo.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VehicleStatsService service = new VehicleStatsService(access, records, documents, objectMapper);

    @Test
    @DisplayName("Statistika meta klaidą kai vartotojas neturi prieigos")
    void statsThrowsWhenUserCannotViewVehicle() {
        Vehicle vehicle = vehicle();

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.stats(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios mašinos");

        verify(records, never()).findAllByVehicleIdOrderByPerformedAtDesc(anyLong());
        verify(documents, never()).findAllByVehicleIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    @DisplayName("Statistika grąžina tuščius sąrašus kai nėra įrašų ir dokumentų")
    void statsReturnsEmptyListsWhenThereAreNoRecordsAndDocuments() {
        Vehicle vehicle = vehicle();

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(records.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(List.of());
        when(documents.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        VehicleStatsResponse result = service.stats(10L, 1L);

        assertThat(result.vehicleId()).isEqualTo(1L);
        assertThat(result.vehicleName()).isEqualTo("Volkswagen Golf (2000)");
        assertThat(result.monthlyCosts()).isEmpty();
        assertThat(result.expenseByType()).isEmpty();
        assertThat(result.fuelHistory()).isEmpty();
        assertThat(result.monthlyFuelAverages()).isEmpty();
    }

    @Test
    @DisplayName("Statistika suskaičiuoja įrašus, dokumentus, kuro istoriją ir kuro vidurkius")
    void statsCalculatesRecordsDocumentsFuelHistoryAndFuelAverages() {
        Vehicle vehicle = vehicle();

        ServiceRecord fuelWithLiters = serviceRecord("FUEL", new BigDecimal("50.00"), "{\"liters\":25}", "2026-03-10T10:00:00Z");
        ServiceRecord fuelWithZeroLiters = serviceRecord("FUEL", BigDecimal.ZERO, "{\"liters\":0}", "2026-01-10T10:00:00Z");
        ServiceRecord fuelWithInvalidJson = serviceRecord("FUEL", new BigDecimal("30.00"), "{invalid-json", "2026-03-12T10:00:00Z");
        ServiceRecord fuelWithoutLiters = serviceRecord("FUEL", new BigDecimal("20.00"), "{}", "2026-03-13T10:00:00Z");
        ServiceRecord serviceRecord = serviceRecord("SERVICE", new BigDecimal("100.00"), "{}", "2026-03-11T10:00:00Z");
        ServiceRecord oilRecordWithNullCost = serviceRecord("OIL", null, "{}", "2026-02-01T10:00:00Z");
        ServiceRecord brakesRecord = serviceRecord("BRAKES", new BigDecimal("80.00"), "{}", "2026-02-02T10:00:00Z");
        ServiceRecord tiresRecord = serviceRecord("TIRES", new BigDecimal("120.00"), "{}", "2026-01-15T10:00:00Z");
        ServiceRecord otherRecord = serviceRecord("WASH", new BigDecimal("10.00"), "{}", "2026-04-01T10:00:00Z");

        VehicleDocument insuranceDocument = document("INSURANCE", "{\"price\":200}", "2026-03-20T10:00:00Z");
        VehicleDocument inspectionDocument = document("INSPECTION", "{\"amount\":30}", "2026-02-20T10:00:00Z");
        VehicleDocument fineDocument = document("FINE", "{\"amount\":15}", "2026-01-20T10:00:00Z");
        VehicleDocument otherDocument = document("OTHER", "{\"amount\":5}", "2026-04-20T10:00:00Z");
        VehicleDocument invalidJsonDocument = document("INSURANCE", "{invalid-json", "2026-05-01T10:00:00Z");
        VehicleDocument zeroPriceDocument = document("INSPECTION", "{\"price\":0}", "2026-05-02T10:00:00Z");
        VehicleDocument negativeAmountDocument = document("FINE", "{\"amount\":-1}", "2026-05-03T10:00:00Z");

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(records.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(List.of(fuelWithLiters, fuelWithZeroLiters, fuelWithInvalidJson, fuelWithoutLiters, serviceRecord, oilRecordWithNullCost, brakesRecord, tiresRecord, otherRecord));
        when(documents.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(insuranceDocument, inspectionDocument, fineDocument, otherDocument, invalidJsonDocument, zeroPriceDocument, negativeAmountDocument));

        VehicleStatsResponse result = service.stats(10L, 1L);

        assertThat(result.vehicleId()).isEqualTo(1L);
        assertThat(result.vehicleName()).isEqualTo("Volkswagen Golf (2000)");

        assertThat(result.monthlyCosts()).containsExactly(
                new VehicleStatsResponse.MonthlyCostPoint("2026-01", new BigDecimal("135.00")),
                new VehicleStatsResponse.MonthlyCostPoint("2026-02", new BigDecimal("110.00")),
                new VehicleStatsResponse.MonthlyCostPoint("2026-03", new BigDecimal("400.00")),
                new VehicleStatsResponse.MonthlyCostPoint("2026-04", new BigDecimal("15.00"))
        );

        assertThat(result.expenseByType()).containsExactly(
                new VehicleStatsResponse.ExpenseByTypePoint("Degalai", new BigDecimal("100.00")),
                new VehicleStatsResponse.ExpenseByTypePoint("Servisas", new BigDecimal("100.00")),
                new VehicleStatsResponse.ExpenseByTypePoint("Tepalai", BigDecimal.ZERO),
                new VehicleStatsResponse.ExpenseByTypePoint("Stabdžiai", new BigDecimal("80.00")),
                new VehicleStatsResponse.ExpenseByTypePoint("Padangos", new BigDecimal("120.00")),
                new VehicleStatsResponse.ExpenseByTypePoint("Kita", new BigDecimal("10.00")),
                new VehicleStatsResponse.ExpenseByTypePoint("Draudimas", new BigDecimal("200")),
                new VehicleStatsResponse.ExpenseByTypePoint("Techninė", new BigDecimal("30")),
                new VehicleStatsResponse.ExpenseByTypePoint("Baudos", new BigDecimal("15")),
                new VehicleStatsResponse.ExpenseByTypePoint("Dokumentai", new BigDecimal("5"))
        );

        assertThat(result.fuelHistory()).containsExactly(
                new VehicleStatsResponse.FuelHistoryPoint("2026-01-10", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new VehicleStatsResponse.FuelHistoryPoint("2026-03-10", new BigDecimal("25"), new BigDecimal("50.00"), new BigDecimal("2.00")),
                new VehicleStatsResponse.FuelHistoryPoint("2026-03-12", BigDecimal.ZERO, new BigDecimal("30.00"), BigDecimal.ZERO),
                new VehicleStatsResponse.FuelHistoryPoint("2026-03-13", BigDecimal.ZERO, new BigDecimal("20.00"), BigDecimal.ZERO)
        );

        assertThat(result.monthlyFuelAverages()).containsExactly(
                new VehicleStatsResponse.MonthlyFuelAveragePoint("2026-01", BigDecimal.ZERO),
                new VehicleStatsResponse.MonthlyFuelAveragePoint("2026-03", new BigDecimal("4.000"))
        );
    }

    @Test
    @DisplayName("Dokumentas be price ir amount nepridedamas prie statistikos")
    void documentWithoutPriceOrAmountIsNotAddedToStats() {
        Vehicle vehicle = vehicle();
        VehicleDocument documentWithoutCost = document("INSURANCE", "{\"note\":\"without price\"}", "2026-03-20T10:00:00Z");

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(records.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(List.of());
        when(documents.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(documentWithoutCost));

        VehicleStatsResponse result = service.stats(10L, 1L);

        assertThat(result.monthlyCosts()).isEmpty();
        assertThat(result.expenseByType()).isEmpty();
    }

    private static Vehicle vehicle() {
        Vehicle vehicle = new Vehicle(10L);
        vehicle.setId(1L);
        vehicle.setMake("Volkswagen");
        vehicle.setModel("Golf");
        vehicle.setModelYear(2000);
        return vehicle;
    }

    private static ServiceRecord serviceRecord(String kind, BigDecimal totalCost, String metaJson, String performedAt) {
        ServiceRecord record = mock(ServiceRecord.class);
        when(record.getKind()).thenReturn(kind);
        when(record.getTotalCost()).thenReturn(totalCost);
        when(record.getMetaJson()).thenReturn(metaJson);
        when(record.getPerformedAt()).thenReturn(Instant.parse(performedAt));
        return record;
    }

    private static VehicleDocument document(String type, String metaJson, String createdAt) {
        VehicleDocument document = mock(VehicleDocument.class);
        when(document.getType()).thenReturn(type);
        when(document.getMetaJson()).thenReturn(metaJson);
        when(document.getCreatedAt()).thenReturn(Instant.parse(createdAt));
        return document;
    }
}