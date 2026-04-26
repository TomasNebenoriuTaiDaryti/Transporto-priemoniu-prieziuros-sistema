package com.example.back.Unit.ai;

import com.example.back.ai.service.VehicleAiContextService;
import com.example.back.document.domain.VehicleDocument;
import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.record.domain.ServiceRecord;
import com.example.back.record.repo.ServiceRecordRepo;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleAiContextServiceTest {

    private final VehicleAccessService vehicleAccessService = mock(VehicleAccessService.class);
    private final ServiceRecordRepo recordRepository = mock(ServiceRecordRepo.class);
    private final VehicleDocumentRepo documentRepository = mock(VehicleDocumentRepo.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VehicleAiContextService service = new VehicleAiContextService(vehicleAccessService, recordRepository, documentRepository, objectMapper);

    @Test
    @DisplayName("Meta klaida kai vartotojas neturi prieigos")
    void buildSanitizedContextThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle();

        when(vehicleAccessService.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(vehicleAccessService.canView(10L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.buildSanitizedContext(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios mašinos");

        verify(recordRepository, never()).findAllByVehicleIdOrderByPerformedAtDesc(anyLong());
        verify(documentRepository, never()).findAllByVehicleIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    @DisplayName("Sukuria konteksta su techniniais duomenimis ir suvestine")
    void buildSanitizedContextCreatesVehicleRecordsDocumentsAndSummary() {
        Vehicle vehicle = vehicle();

        ServiceRecord fuel = record("FUEL", "Degalai", "2026-01-02T10:00:00Z", 1000L, new BigDecimal("50.00"), "EUR", "{\"liters\":25}", "Fuel desc");
        ServiceRecord oil = record("OIL", "Tepalai", null, 2000L, null, "EUR", "{invalid-json", "Oil desc");
        ServiceRecord serviceRecord = record("SERVICE", "Servisas", "2026-01-03T10:00:00Z", null, new BigDecimal("100.00"), null, "{}", "");

        VehicleDocument insurance = document("INSURANCE", "Draudimas", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{\"price\":100}", "Insurance desc");
        VehicleDocument fine = document("FINE", "Bauda", LocalDate.of(2026, 2, 1), null, "{\"amount\":30}", "");
        VehicleDocument otherInvalid = document("OTHER", "Other", null, null, "{invalid-json", "Other desc");

        when(vehicleAccessService.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(vehicleAccessService.canView(10L, vehicle)).thenReturn(true);
        when(recordRepository.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(List.of(fuel, oil, serviceRecord));
        when(documentRepository.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(insurance, fine, otherInvalid));

        String result = service.buildSanitizedContext(10L, 1L);

        assertThat(result).contains("AUTOMOBILIO KONTEKSTAS");
        assertThat(result).contains("Svarbu: šiame kontekste nėra asmens duomenų ir nėra VIN.");
        assertThat(result).contains("- Markė: Volkswagen");
        assertThat(result).contains("- Modelis: Golf");
        assertThat(result).contains("- Metai: 2000");
        assertThat(result).contains("- Variklio darbinis tūris ccm: 1900");
        assertThat(result).contains("- Kuras: DIESEL");
        assertThat(result).contains("- Pavarų dėžė: Manual");
        assertThat(result).contains("- Varantieji ratai: FWD");
        assertThat(result).contains("- Kėbulas: Hatchback");
        assertThat(result).contains("- Durys: 5");
        assertThat(result).contains("- Vietos: 5");
        assertThat(result).contains("- CO2 g/km: 140.50");
        assertThat(result).contains("- Gamintojo šalis: Germany");
        assertThat(result).contains("- Gamintojas: Volkswagen AG");
        assertThat(result).contains("- Dabartinė rida km: 123000");

        assertThat(result).contains("- [FUEL] data=2026-01-02, pavadinimas=Degalai, rida=1000, kaina=50.00 EUR, litrai=25, pastabos=Fuel desc");
        assertThat(result).contains("- [OIL] data=-, pavadinimas=Tepalai, rida=2000, kaina=0 EUR, pastabos=Oil desc");
        assertThat(result).contains("- [SERVICE] data=2026-01-03, pavadinimas=Servisas, rida=-, kaina=100.00 -");

        assertThat(result).contains("- [INSURANCE] pavadinimas=Draudimas, nuo=2026-01-01, iki=2027-01-01, suma=100, pastabos=Insurance desc");
        assertThat(result).contains("- [FINE] pavadinimas=Bauda, nuo=2026-02-01, iki=-, suma=30");
        assertThat(result).contains("- [OTHER] pavadinimas=Other, nuo=-, iki=-, suma=0, pastabos=Other desc");

        assertThat(result).contains("- Bendra įrašų suma: 150.00");
        assertThat(result).contains("- Bendra dokumentų suma: 130");
        assertThat(result).contains("- Bendra degalų suma: 50.00");
        assertThat(result).contains("- Bendra degalų litrų suma: 25");
        assertThat(result).contains("- Įrašų tipų kiekiai: {FUEL=1, OIL=1, SERVICE=1}");
        assertThat(result).contains("- Dokumentų tipų kiekiai: {INSURANCE=1, FINE=1, OTHER=1}");
    }

    @Test
    @DisplayName("Kontekste rodo tik pirmus 20 irasu ir dokumentu")
    void buildSanitizedContextListsOnlyFirstTwentyRecordsAndDocuments() {
        Vehicle vehicle = vehicle();
        List<ServiceRecord> records = new ArrayList<>();
        List<VehicleDocument> documents = new ArrayList<>();

        for (int i = 1; i <= 21; i++) {
            records.add(record("OTHER", "Record " + i, "2026-01-01T10:00:00Z", 1000L, BigDecimal.ONE, "EUR", "{}", null));
            documents.add(document("OTHER", "Document " + i, null, null, "{}", null));
        }

        when(vehicleAccessService.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(vehicleAccessService.canView(10L, vehicle)).thenReturn(true);
        when(recordRepository.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(records);
        when(documentRepository.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(documents);

        String result = service.buildSanitizedContext(10L, 1L);

        assertThat(result).contains("Record 20");
        assertThat(result).doesNotContain("Record 21");
        assertThat(result).contains("Document 20");
        assertThat(result).doesNotContain("Document 21");
        assertThat(result).contains("- Bendra įrašų suma: 21");
        assertThat(result).contains("- Bendra dokumentų suma: 0");
    }

    @Test
    @DisplayName("Null techniniai laukai rodomi kaip bruksnys")
    void buildSanitizedContextShowsDashForNullVehicleFields() {
        Vehicle vehicle = new Vehicle(10L);
        vehicle.setId(1L);

        when(vehicleAccessService.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(vehicleAccessService.canView(10L, vehicle)).thenReturn(true);
        when(recordRepository.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(List.of());
        when(documentRepository.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        String result = service.buildSanitizedContext(10L, 1L);

        assertThat(result).contains("- Markė: -");
        assertThat(result).contains("- Modelis: -");
        assertThat(result).contains("- Dabartinė rida km: 0");
        assertThat(result).contains("- Bendra įrašų suma: 0");
        assertThat(result).contains("- Bendra dokumentų suma: 0");
    }

    private static Vehicle vehicle() {
        Vehicle vehicle = new Vehicle(10L);
        vehicle.setId(1L);
        vehicle.setMake("Volkswagen");
        vehicle.setModel("Golf");
        vehicle.setModelYear(2000);
        vehicle.setEngineDisplacementCc(1900);
        vehicle.setFuelType("DIESEL");
        vehicle.setTransmission("Manual");
        vehicle.setDrive("FWD");
        vehicle.setBody("Hatchback");
        vehicle.setDoors(5);
        vehicle.setSeats(5);
        vehicle.setCo2Gkm(new BigDecimal("140.50"));
        vehicle.setPlantCountry("Germany");
        vehicle.setManufacturer("Volkswagen AG");
        vehicle.setOdometerKm(123000L);
        return vehicle;
    }

    private static ServiceRecord record(String kind, String title, String performedAt, Long odometerKm, BigDecimal totalCost, String currency, String metaJson, String description) {
        ServiceRecord record = new ServiceRecord();
        record.setKind(kind);
        record.setTitle(title);
        record.setPerformedAt(performedAt == null ? null : Instant.parse(performedAt));
        record.setOdometerKm(odometerKm);
        record.setTotalCost(totalCost);
        record.setCurrency(currency);
        record.setMetaJson(metaJson);
        record.setDescription(description);
        return record;
    }

    private static VehicleDocument document(String type, String title, LocalDate issueDate, LocalDate expiresAt, String metaJson, String description) {
        VehicleDocument document = new VehicleDocument();
        document.setType(type);
        document.setTitle(title);
        document.setIssueDate(issueDate);
        document.setExpiresAt(expiresAt);
        document.setMetaJson(metaJson);
        document.setDescription(description);
        return document;
    }
}