package com.example.back.reminder.service;

import com.example.back.reminder.domain.Reminder;
import com.example.back.reminder.dto.ReminderDtos;
import com.example.back.reminder.repo.ReminderRepo;
import com.example.back.security.AuthUser;
import com.example.back.vehicle.service.VehicleAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReminderService {

    private final ReminderRepo reminders;
    private final VehicleAccessService access;

    public ReminderService(ReminderRepo reminders, VehicleAccessService access) {
        this.reminders = reminders;
        this.access = access;
    }

    public List<ReminderDtos.ReminderResponse> list(Long vehicleId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        var vehicle = access.getVehicleOrThrow(vehicleId);

        if (!access.isVehicleOwner(userId, vehicle)) {
            return List.of();
        }
        return reminders.findAllByVehicleIdAndStatusInOrderByDueAtAscDueOdometerKmAsc(vehicleId, List.of("SCHEDULED", "TRIGGERED")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteBySource(Long vehicleId, String sourceType, String sourceId) {
        reminders.deleteByVehicleIdAndSourceTypeAndSourceId(vehicleId, sourceType, sourceId);
    }

    @Transactional
    public void upsertDocumentReminder(Long vehicleId, String sourceType, String sourceId, String title, LocalDate endDate) {
        if (endDate == null) return;
        Instant due = endDate.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant();
        upsertDateBasedReminder(vehicleId, sourceType, sourceId, title, title + " galioja iki " + endDate, due, 30, 0L);
    }

    @Transactional
    public void upsertDateReminder(Long vehicleId, String sourceType, String sourceId, String title, LocalDate dueDate) {
        if (dueDate == null) return;
        Instant due = dueDate.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant();
        upsertDateBasedReminder(vehicleId, sourceType, sourceId, title, "Artėja: " + title, due, 30, 0L);
    }

    @Transactional
    public void upsertMileageReminder(Long vehicleId, String sourceType, String sourceId, String title, Long dueOdoKm) {
        if (dueOdoKm == null) return;
        Reminder reminder = reminders.findByVehicleIdAndSourceTypeAndSourceId(vehicleId, sourceType, sourceId).orElseGet(() -> new Reminder(vehicleId, title, "Artėja: " + title + " (liko apie 3000km)", null, dueOdoKm, 0, 3000L, sourceType, sourceId));
        reminder.updateMileageReminder(title, "Artėja: " + title + " (liko apie 3000km)", dueOdoKm, 0, 3000L);
        reminders.save(reminder);
    }

    private void upsertDateBasedReminder(Long vehicleId, String sourceType, String sourceId, String title, String message, Instant due, Integer notifyBeforeDays, Long notifyBeforeKm
    ) {
        Reminder reminder = reminders.findByVehicleIdAndSourceTypeAndSourceId(vehicleId, sourceType, sourceId).orElseGet(() -> new Reminder(vehicleId, title, message, due, null, notifyBeforeDays, notifyBeforeKm, sourceType, sourceId));
        reminder.updateDateReminder(title, message, due, notifyBeforeDays, notifyBeforeKm);
        reminders.save(reminder);
    }

    private ReminderDtos.ReminderResponse toResponse(Reminder r) {
        return new ReminderDtos.ReminderResponse(r.getId(), r.getTitle(), r.getMessage(), r.getDueAt(), r.getDueOdometerKm(), r.getNotifyBeforeDays(), r.getNotifyBeforeKm(), r.getStatus());
    }
}