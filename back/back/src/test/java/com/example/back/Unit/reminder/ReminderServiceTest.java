package com.example.back.Unit.reminder;

import com.example.back.reminder.domain.Reminder;
import com.example.back.reminder.dto.ReminderDtos;
import com.example.back.reminder.repo.ReminderRepo;
import com.example.back.reminder.service.ReminderService;
import com.example.back.security.AuthUser;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderServiceTest {

    private final ReminderRepo reminderRepo = mock(ReminderRepo.class);
    private final VehicleAccessService access = mock(VehicleAccessService.class);
    private final ReminderService service = new ReminderService(reminderRepo, access);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Savininkui grazina priminimu sarasa")
    void listReturnsRemindersForOwner() {
        Vehicle vehicle = new Vehicle(10L);
        vehicle.setId(1L);

        Reminder dateReminder = new Reminder(1L, "Draudimas", "Draudimas galioja iki 2026-05-20", Instant.parse("2026-05-20T06:00:00Z"), null, 30, 0L, "DOCUMENT", "doc-1");
        setId(dateReminder, 100L);

        Reminder mileageReminder = new Reminder(1L, "Tepalai", "Artėja: Tepalai (liko apie 3000km)", null, 150000L, 0, 3000L, "RECORD_OIL_KM", "rec-1");
        setId(mileageReminder, 101L);
        setField(mileageReminder, "status", "TRIGGERED");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
            when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);
            when(reminderRepo.findAllByVehicleIdAndStatusInOrderByDueAtAscDueOdometerKmAsc(eq(1L), eq(List.of("SCHEDULED", "TRIGGERED")))).thenReturn(List.of(dateReminder, mileageReminder));
            List<ReminderDtos.ReminderResponse> result = service.list(1L, auth);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(100L);
            assertThat(result.get(0).title()).isEqualTo("Draudimas");
            assertThat(result.get(0).message()).isEqualTo("Draudimas galioja iki 2026-05-20");
            assertThat(result.get(0).dueAt()).isEqualTo(Instant.parse("2026-05-20T06:00:00Z"));
            assertThat(result.get(0).dueOdometerKm()).isNull();
            assertThat(result.get(0).notifyBeforeDays()).isEqualTo(30);
            assertThat(result.get(0).notifyBeforeKm()).isEqualTo(0L);
            assertThat(result.get(0).status()).isEqualTo("SCHEDULED");
            assertThat(result.get(1).id()).isEqualTo(101L);
            assertThat(result.get(1).title()).isEqualTo("Tepalai");
            assertThat(result.get(1).dueAt()).isNull();
            assertThat(result.get(1).dueOdometerKm()).isEqualTo(150000L);
            assertThat(result.get(1).status()).isEqualTo("TRIGGERED");
            verify(access).getVehicleOrThrow(1L);
            verify(access).isVehicleOwner(10L, vehicle);
            verify(reminderRepo).findAllByVehicleIdAndStatusInOrderByDueAtAscDueOdometerKmAsc(1L, List.of("SCHEDULED", "TRIGGERED"));
        }
    }

    @Test
    @DisplayName("Ne savininkui grazina tuscia sarasa")
    void listReturnsEmptyListForNonOwner() {
        Vehicle vehicle = new Vehicle(20L);
        vehicle.setId(1L);

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
            when(access.isVehicleOwner(10L, vehicle)).thenReturn(false);
            List<ReminderDtos.ReminderResponse> result = service.list(1L, auth);

            assertThat(result).isEmpty();
            verify(access).getVehicleOrThrow(1L);
            verify(access).isVehicleOwner(10L, vehicle);
            verifyNoInteractions(reminderRepo);
        }
    }

    @Test
    @DisplayName("Kai masina nerasta perduodama klaida")
    void listPropagatesVehicleNotFoundException() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(access.getVehicleOrThrow(1L)).thenThrow(new IllegalArgumentException("Mašina nerasta"));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.list(1L, auth)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");

            verify(access).getVehicleOrThrow(1L);
            verify(access, never()).isVehicleOwner(anyLong(), any());
            verifyNoInteractions(reminderRepo);
        }
    }

    @Test
    @DisplayName("Istrina priminima pagal saltini")
    void deleteBySourceDeletesReminderBySource() {
        service.deleteBySource(1L, "DOCUMENT", "doc-1");

        verify(reminderRepo).deleteByVehicleIdAndSourceTypeAndSourceId(1L, "DOCUMENT", "doc-1");
        verifyNoMoreInteractions(reminderRepo);
    }

    @Test
    @DisplayName("Dokumento priminimas nieko nedaro kai pabaigos data null")
    void upsertDocumentReminderDoesNothingWhenEndDateIsNull() {
        service.upsertDocumentReminder(1L, "DOCUMENT", "doc-1", "Draudimas", null);

        verifyNoInteractions(reminderRepo);
    }

    @Test
    @DisplayName("Sukuria nauja dokumento priminima")
    void upsertDocumentReminderCreatesNewReminder() {
        LocalDate endDate = LocalDate.of(2026, 5, 20);
        when(reminderRepo.findByVehicleIdAndSourceTypeAndSourceId(1L, "DOCUMENT", "doc-1")).thenReturn(Optional.empty());
        service.upsertDocumentReminder(1L, "DOCUMENT", "doc-1", "Draudimas", endDate);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepo).findByVehicleIdAndSourceTypeAndSourceId(1L, "DOCUMENT", "doc-1");
        verify(reminderRepo).save(captor.capture());

        Reminder saved = captor.getValue();
        assertThat(saved.getVehicleId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("Draudimas");
        assertThat(saved.getMessage()).isEqualTo("Draudimas galioja iki 2026-05-20");
        assertThat(saved.getDueAt()).isEqualTo(toVilniusInstant(endDate));
        assertThat(saved.getDueOdometerKm()).isNull();
        assertThat(saved.getNotifyBeforeDays()).isEqualTo(30);
        assertThat(saved.getNotifyBeforeKm()).isEqualTo(0L);
        assertThat(saved.getStatus()).isEqualTo("SCHEDULED");
        assertThat(saved.getSourceType()).isEqualTo("DOCUMENT");
        assertThat(saved.getSourceId()).isEqualTo("doc-1");
    }

    @Test
    @DisplayName("Atnaujina esama dokumento priminima")
    void upsertDocumentReminderUpdatesExistingReminder() {
        LocalDate endDate = LocalDate.of(2026, 5, 20);
        Reminder existing = new Reminder(1L, "Senas", "Sena zinute", Instant.parse("2026-01-01T07:00:00Z"), null, 7, 300L, "DOCUMENT", "doc-1");
        when(reminderRepo.findByVehicleIdAndSourceTypeAndSourceId(1L, "DOCUMENT", "doc-1")).thenReturn(Optional.of(existing));
        service.upsertDocumentReminder(1L, "DOCUMENT", "doc-1", "Draudimas", endDate);

        verify(reminderRepo).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Draudimas");
        assertThat(existing.getMessage()).isEqualTo("Draudimas galioja iki 2026-05-20");
        assertThat(existing.getDueAt()).isEqualTo(toVilniusInstant(endDate));
        assertThat(existing.getDueOdometerKm()).isNull();
        assertThat(existing.getNotifyBeforeDays()).isEqualTo(30);
        assertThat(existing.getNotifyBeforeKm()).isEqualTo(0L);
        assertThat(existing.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("Nekuria priminimo kai data nepateikta")
    void upsertDateReminderDoesNothingWhenDueDateIsNull() {
        service.upsertDateReminder(1L, "RECORD_OIL_DATE", "rec-1", "Tepalai", null);

        verifyNoInteractions(reminderRepo);
    }

    @Test
    @DisplayName("Sukuria priminima pagal data")
    void upsertDateReminderCreatesNewReminder() {
        LocalDate dueDate = LocalDate.of(2026, 6, 15);
        when(reminderRepo.findByVehicleIdAndSourceTypeAndSourceId(1L, "RECORD_OIL_DATE", "rec-1")).thenReturn(Optional.empty());
        service.upsertDateReminder(1L, "RECORD_OIL_DATE", "rec-1", "Tepalai", dueDate);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepo).save(captor.capture());

        Reminder saved = captor.getValue();
        assertThat(saved.getVehicleId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("Tepalai");
        assertThat(saved.getMessage()).isEqualTo("Artėja: Tepalai");
        assertThat(saved.getDueAt()).isEqualTo(toVilniusInstant(dueDate));
        assertThat(saved.getDueOdometerKm()).isNull();
        assertThat(saved.getNotifyBeforeDays()).isEqualTo(30);
        assertThat(saved.getNotifyBeforeKm()).isEqualTo(0L);
        assertThat(saved.getStatus()).isEqualTo("SCHEDULED");
        assertThat(saved.getSourceType()).isEqualTo("RECORD_OIL_DATE");
        assertThat(saved.getSourceId()).isEqualTo("rec-1");
    }

    @Test
    @DisplayName("Atnaujina priminima pagal data")
    void upsertDateReminderUpdatesExistingReminder() {
        LocalDate dueDate = LocalDate.of(2026, 6, 15);
        Reminder existing = new Reminder(1L, "Senas", "Sena zinute", null, 120000L, 0, 3000L, "RECORD_OIL_DATE", "rec-1");
        when(reminderRepo.findByVehicleIdAndSourceTypeAndSourceId(1L, "RECORD_OIL_DATE", "rec-1")).thenReturn(Optional.of(existing));
        service.upsertDateReminder(1L, "RECORD_OIL_DATE", "rec-1", "Tepalai", dueDate);

        verify(reminderRepo).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Tepalai");
        assertThat(existing.getMessage()).isEqualTo("Artėja: Tepalai");
        assertThat(existing.getDueAt()).isEqualTo(toVilniusInstant(dueDate));
        assertThat(existing.getDueOdometerKm()).isNull();
        assertThat(existing.getNotifyBeforeDays()).isEqualTo(30);
        assertThat(existing.getNotifyBeforeKm()).isEqualTo(0L);
        assertThat(existing.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("Nekuria priminimo kai rida nepateikta")
    void upsertMileageReminderDoesNothingWhenDueOdoKmIsNull() {
        service.upsertMileageReminder(1L, "RECORD_OIL_KM", "rec-1", "Tepalai", null);

        verifyNoInteractions(reminderRepo);
    }

    @Test
    @DisplayName("Sukuria priminima pagal rida")
    void upsertMileageReminderCreatesNewReminder() {
        when(reminderRepo.findByVehicleIdAndSourceTypeAndSourceId(1L, "RECORD_OIL_KM", "rec-1")).thenReturn(Optional.empty());
        service.upsertMileageReminder(1L, "RECORD_OIL_KM", "rec-1", "Tepalai", 150000L);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepo).save(captor.capture());

        Reminder saved = captor.getValue();
        assertThat(saved.getVehicleId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("Tepalai");
        assertThat(saved.getMessage()).isEqualTo("Artėja: Tepalai (liko apie 3000km)");
        assertThat(saved.getDueAt()).isNull();
        assertThat(saved.getDueOdometerKm()).isEqualTo(150000L);
        assertThat(saved.getNotifyBeforeDays()).isEqualTo(0);
        assertThat(saved.getNotifyBeforeKm()).isEqualTo(3000L);
        assertThat(saved.getStatus()).isEqualTo("SCHEDULED");
        assertThat(saved.getSourceType()).isEqualTo("RECORD_OIL_KM");
        assertThat(saved.getSourceId()).isEqualTo("rec-1");
    }

    @Test
    @DisplayName("Atnaujina priminima pagal rida")
    void upsertMileageReminderUpdatesExistingReminder() {
        Reminder existing = new Reminder(1L, "Senas", "Sena zinute", Instant.parse("2026-01-01T07:00:00Z"), null, 30, 0L, "RECORD_OIL_KM", "rec-1");
        when(reminderRepo.findByVehicleIdAndSourceTypeAndSourceId(1L, "RECORD_OIL_KM", "rec-1")).thenReturn(Optional.of(existing));
        service.upsertMileageReminder(1L, "RECORD_OIL_KM", "rec-1", "Tepalai", 150000L);

        verify(reminderRepo).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Tepalai");
        assertThat(existing.getMessage()).isEqualTo("Artėja: Tepalai (liko apie 3000km)");
        assertThat(existing.getDueAt()).isNull();
        assertThat(existing.getDueOdometerKm()).isEqualTo(150000L);
        assertThat(existing.getNotifyBeforeDays()).isEqualTo(0);
        assertThat(existing.getNotifyBeforeKm()).isEqualTo(3000L);
        assertThat(existing.getStatus()).isEqualTo("SCHEDULED");
    }

    private static Instant toVilniusInstant(LocalDate date) {
        return date.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant();
    }

    private static void setId(Reminder reminder, Long id) {
        setField(reminder, "id", id);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new AssertionError("Nepavyko nustatyti lauko: " + fieldName, e);
        }
    }
}