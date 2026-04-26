package com.example.back.Unit.record;

import com.example.back.record.domain.ServiceRecord;
import com.example.back.record.dto.RecordDtos;
import com.example.back.record.repo.ServiceRecordRepo;
import com.example.back.record.service.RecordService;
import com.example.back.reminder.service.ReminderPlanner;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordServiceTest {

    private final ServiceRecordRepo records = mock(ServiceRecordRepo.class);
    private final VehicleAccessService access = mock(VehicleAccessService.class);
    private final ReminderPlanner planner = mock(ReminderPlanner.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RecordService service = new RecordService(records, access, planner, objectMapper);

    @Test
    @DisplayName("Sarasas grazinamas kai vartotojas turi prieiga")
    void listReturnsRecordsWhenUserCanView() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(5L, 1L, 20L, "OIL", "Title", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.findAllByVehicleIdOrderByPerformedAtDesc(1L)).thenReturn(List.of(record));

        List<RecordDtos.RecordResponse> result = service.list(20L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(5L);
        assertThat(result.get(0).kind()).isEqualTo(RecordDtos.RecordKind.OIL);
        assertThat(result.get(0).canEdit()).isTrue();
    }

    @Test
    @DisplayName("Sarasas meta klaida kai nera prieigos")
    void listThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.list(20L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");

        verify(records, never()).findAllByVehicleIdOrderByPerformedAtDesc(anyLong());
    }

    @Test
    @DisplayName("Sukuria irasa su default data ir rida")
    void createUsesDefaultPerformedAtAndOdometer() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.OTHER, null, null, null, null, new BigDecimal("15.00"), "EUR", null, null, null);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.save(any(ServiceRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        RecordDtos.RecordResponse result = service.create(20L, 1L, request);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.vehicleId()).isEqualTo(1L);
        assertThat(result.createdByUserId()).isEqualTo(20L);
        assertThat(result.kind()).isEqualTo(RecordDtos.RecordKind.OTHER);
        assertThat(result.title()).isEqualTo("Kitas įrašas");
        assertThat(result.odometerKm()).isEqualTo(5000L);
        assertThat(result.performedAt()).isNotNull();
        assertThat(result.metaJson()).isEqualTo("{}");
        assertThat(result.canEdit()).isTrue();
    }

    @Test
    @DisplayName("Sukuria OTHER irasa su custom pavadinimu")
    void createOtherUsesCustomTitle() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.OTHER, "Custom", "Desc", Instant.parse("2026-01-01T10:00:00Z"), 6000L, new BigDecimal("15.00"), "EUR", null, null, null);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.save(any(ServiceRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        RecordDtos.RecordResponse result = service.create(20L, 1L, request);

        assertThat(result.title()).isEqualTo("Custom");
        assertThat(result.description()).isEqualTo("Desc");
        assertThat(result.odometerKm()).isEqualTo(6000L);
    }

    @Test
    @DisplayName("Sukuria OIL irasa ir priminimus")
    void createOilCreatesReminders() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        Instant performedAt = Instant.parse("2026-01-01T10:00:00Z");
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.OIL, null, "Desc", performedAt, 6000L, new BigDecimal("50.00"), "EUR", null, null, null);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.save(any(ServiceRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        RecordDtos.RecordResponse result = service.create(20L, 1L, request);

        assertThat(result.title()).isEqualTo("Tepalų ir filtro keitimas");
        verify(planner).upsertMileageReminder(1L, "RECORD_OIL_KM", "7", "Tepalų keitimas (rida)", 16000L);
        verify(planner).upsertDateReminder(1L, "RECORD_OIL_DATE", "7", "Tepalų keitimas (laikas)", LocalDate.ofInstant(performedAt, ZoneId.of("Europe/Vilnius")).plusYears(1));
    }

    @Test
    @DisplayName("Sukuria BRAKES irasa ir ridos priminima")
    void createBrakesCreatesMileageReminder() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.BRAKES, null, null, Instant.parse("2026-01-01T10:00:00Z"), 6000L, new BigDecimal("80.00"), "EUR", null, null, null);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.save(any(ServiceRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        RecordDtos.RecordResponse result = service.create(20L, 1L, request);

        assertThat(result.title()).isEqualTo("Stabdžių aptarnavimas");
        verify(planner).upsertMileageReminder(1L, "RECORD_BRAKES_KM", "7", "Stabdžiai", 46000L);
    }

    @Test
    @DisplayName("Sukuria TIRES irasa ir datos priminima")
    void createTiresCreatesDateReminder() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.TIRES, null, null, Instant.parse("2026-01-01T10:00:00Z"), 6000L, new BigDecimal("80.00"), "EUR", "WINTER", 2, null);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.save(any(ServiceRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        RecordDtos.RecordResponse result = service.create(20L, 1L, request);

        assertThat(result.title()).isEqualTo("Padangų keitimas");
        assertThat(result.metaJson()).contains("\"tireType\":\"WINTER\"");
        assertThat(result.metaJson()).contains("\"tireAgeYears\":2");
        verify(planner).upsertDateReminder(eq(1L), eq("RECORD_TIRES_DATE"), eq("7"), eq("Padangos (amžius)"), any(LocalDate.class));
    }

    @Test
    @DisplayName("Sukuria FUEL irasa su litrais meta JSON")
    void createFuelStoresLitersInMetaJson() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.FUEL, null, null, Instant.parse("2026-01-01T10:00:00Z"), 6000L, new BigDecimal("40.00"), "EUR", null, null, new BigDecimal("25.5"));

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(records.save(any(ServiceRecord.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        RecordDtos.RecordResponse result = service.create(20L, 1L, request);

        assertThat(result.title()).isEqualTo("Degalų pildymas");
        assertThat(result.metaJson()).contains("\"liters\":25.5");
        verifyNoInteractions(planner);
    }

    @Test
    @DisplayName("Sukurimas meta klaida kai nera prieigos")
    void createThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.create(20L, 1L, createRequest(RecordDtos.RecordKind.SERVICE, null, null, null, null, BigDecimal.ZERO, "EUR", null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");

        verify(records, never()).save(any());
    }

    @Test
    @DisplayName("Atnaujina irasa kai vartotojas yra savininkas")
    void updateWorksWhenUserIsOwner() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);
        RecordDtos.UpdateRecordRequest request = updateRequest(RecordDtos.RecordKind.SERVICE, "Ignored", "New desc", Instant.parse("2026-02-01T10:00:00Z"), 7000L, new BigDecimal("100.00"), "EUR", null, null, null);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);

        RecordDtos.RecordResponse result = service.update(10L, 7L, request);

        assertThat(result.title()).isEqualTo("Apsilankymas servise");
        assertThat(result.description()).isEqualTo("New desc");
        assertThat(result.odometerKm()).isEqualTo(7000L);
        verifyReminderDeletes();
    }

    @Test
    @DisplayName("Atnaujina irasa kai vartotojas yra kurejas")
    void updateWorksWhenUserIsCreator() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);
        RecordDtos.UpdateRecordRequest request = updateRequest(RecordDtos.RecordKind.OTHER, "Custom", "Desc", Instant.parse("2026-02-01T10:00:00Z"), 7000L, new BigDecimal("100.00"), "EUR", null, null, null);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(20L, vehicle)).thenReturn(false);

        RecordDtos.RecordResponse result = service.update(20L, 7L, request);

        assertThat(result.title()).isEqualTo("Custom");
        assertThat(result.canEdit()).isTrue();
    }

    @Test
    @DisplayName("Atnaujinimas meta klaida kai irasas nerastas")
    void updateThrowsWhenRecordMissing() {
        when(records.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, 7L, updateRequest(RecordDtos.RecordKind.SERVICE, null, null, Instant.parse("2026-01-01T10:00:00Z"), 1000L, BigDecimal.ZERO, "EUR", null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Įrašas nerastas");
    }

    @Test
    @DisplayName("Atnaujinimas meta klaida kai nera prieigos")
    void updateThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.update(30L, 7L, updateRequest(RecordDtos.RecordKind.SERVICE, null, null, Instant.parse("2026-01-01T10:00:00Z"), 1000L, BigDecimal.ZERO, "EUR", null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");
    }

    @Test
    @DisplayName("Atnaujinimas meta klaida kai negali redaguoti")
    void updateThrowsWhenUserCannotEdit() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.update(30L, 7L, updateRequest(RecordDtos.RecordKind.SERVICE, null, null, Instant.parse("2026-01-01T10:00:00Z"), 1000L, BigDecimal.ZERO, "EUR", null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Negali redaguoti");
    }

    @Test
    @DisplayName("Istrina irasa kai vartotojas yra savininkas")
    void deleteWorksWhenUserIsOwner() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);

        service.delete(10L, 7L);

        verifyReminderDeletes();
        verify(records).delete(record);
    }

    @Test
    @DisplayName("Istrina irasa kai vartotojas yra kurejas")
    void deleteWorksWhenUserIsCreator() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(20L, vehicle)).thenReturn(false);

        service.delete(20L, 7L);

        verify(records).delete(record);
    }

    @Test
    @DisplayName("Trynimas meta klaida kai irasas nerastas")
    void deleteThrowsWhenRecordMissing() {
        when(records.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, 7L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Įrašas nerastas");
    }

    @Test
    @DisplayName("Trynimas meta klaida kai nera prieigos")
    void deleteThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(30L, 7L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");
    }

    @Test
    @DisplayName("Trynimas meta klaida kai negali trinti")
    void deleteThrowsWhenUserCannotEdit() {
        Vehicle vehicle = vehicle(1L, 10L, 5000L);
        ServiceRecord record = record(7L, 1L, 20L, "SERVICE", "Old", Instant.parse("2026-01-01T10:00:00Z"), 5000L);

        when(records.findById(7L)).thenReturn(Optional.of(record));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(30L, 7L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Negali trinti");
    }

    private void verifyReminderDeletes() {
        verify(planner).deleteBySource(1L, "RECORD_OIL_KM", "7");
        verify(planner).deleteBySource(1L, "RECORD_OIL_DATE", "7");
        verify(planner).deleteBySource(1L, "RECORD_BRAKES_KM", "7");
        verify(planner).deleteBySource(1L, "RECORD_TIRES_DATE", "7");
    }

    private static Vehicle vehicle(Long id, Long ownerUserId, Long odometerKm) {
        Vehicle vehicle = new Vehicle(ownerUserId);
        vehicle.setId(id);
        vehicle.setOdometerKm(odometerKm);
        return vehicle;
    }

    private static ServiceRecord record(Long id, Long vehicleId, Long createdByUserId, String kind, String title, Instant performedAt, Long odometerKm) {
        ServiceRecord record = new ServiceRecord();
        record.setVehicleId(vehicleId);
        record.setCreatedByUserId(createdByUserId);
        record.setKind(kind);
        record.setType("MAINTENANCE");
        record.setTitle(title);
        record.setDescription("Description");
        record.setPerformedAt(performedAt);
        record.setOdometerKm(odometerKm);
        record.setTotalCost(new BigDecimal("50.00"));
        record.setCurrency("EUR");
        record.setMetaJson("{}");
        return withId(record, id);
    }

    private static RecordDtos.CreateRecordRequest createRequest(RecordDtos.RecordKind kind, String title, String description, Instant performedAt, Long odometerKm, BigDecimal totalCost, String currency, String tireType, Integer tireAgeYears, BigDecimal liters) {
        return new RecordDtos.CreateRecordRequest(kind, title, description, performedAt, odometerKm, totalCost, currency, tireType, tireAgeYears, liters);
    }

    private static RecordDtos.UpdateRecordRequest updateRequest(RecordDtos.RecordKind kind, String title, String description, Instant performedAt, Long odometerKm, BigDecimal totalCost, String currency, String tireType, Integer tireAgeYears, BigDecimal liters) {
        return new RecordDtos.UpdateRecordRequest(kind, title, description, performedAt, odometerKm, totalCost, currency, tireType, tireAgeYears, liters);
    }

    private static ServiceRecord withId(ServiceRecord record, Long id) {
        try {
            Field field = ServiceRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
            return record;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}