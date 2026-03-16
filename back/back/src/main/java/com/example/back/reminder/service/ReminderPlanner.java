package com.example.back.reminder.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ReminderPlanner {

    private final JdbcTemplate jdbc;

    public ReminderPlanner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void deleteBySource(Long vehicleId, String sourceType, String sourceId) {
        jdbc.update("""
      delete from reminder
      where vehicle_id = ?
        and source_type = ?
        and source_id = ?
      """, vehicleId, sourceType, sourceId);
    }

    public void upsertDocumentReminder(Long vehicleId, String sourceType, String sourceId, String title, LocalDate endDate) {
        if (endDate == null) return;

        Instant due = endDate.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant();

        jdbc.update("""
      insert into reminder(vehicle_id, title, message, due_at, notify_before_days, notify_before_km, status, source_type, source_id)
      values (?, ?, ?, ?, 30, 0, 'SCHEDULED', ?, ?)
      on conflict on constraint uq_reminder_source
      do update set
        title = excluded.title,
        message = excluded.message,
        due_at = excluded.due_at,
        due_odometer_km = null,
        notify_before_days = excluded.notify_before_days,
        notify_before_km = excluded.notify_before_km,
        status = 'SCHEDULED'
      """, vehicleId, title, title + " galioja iki " + endDate, Timestamp.from(due), sourceType, sourceId);
    }

    public void upsertMileageReminder(Long vehicleId, String sourceType, String sourceId, String title, Long dueOdoKm) {
        if (dueOdoKm == null) return;

        jdbc.update("""
      insert into reminder(vehicle_id, title, message, due_odometer_km, notify_before_days, notify_before_km, status, source_type, source_id)
      values (?, ?, ?, ?, 0, 3000, 'SCHEDULED', ?, ?)
      on conflict on constraint uq_reminder_source
      do update set
        title = excluded.title,
        message = excluded.message,
        due_odometer_km = excluded.due_odometer_km,
        due_at = null,
        notify_before_days = excluded.notify_before_days,
        notify_before_km = excluded.notify_before_km,
        status = 'SCHEDULED'
      """, vehicleId, title, "Artėja: " + title + " (liko apie 3000km)", dueOdoKm, sourceType, sourceId);
    }

    public void upsertDateReminder(Long vehicleId, String sourceType, String sourceId, String title, LocalDate dueDate) {
        if (dueDate == null) return;

        Instant due = dueDate.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant();

        jdbc.update("""
      insert into reminder(vehicle_id, title, message, due_at, notify_before_days, notify_before_km, status, source_type, source_id)
      values (?, ?, ?, ?, 30, 0, 'SCHEDULED', ?, ?)
      on conflict on constraint uq_reminder_source
      do update set
        title = excluded.title,
        message = excluded.message,
        due_at = excluded.due_at,
        due_odometer_km = null,
        notify_before_days = excluded.notify_before_days,
        notify_before_km = excluded.notify_before_km,
        status = 'SCHEDULED'
      """, vehicleId, title, "Artėja: " + title, Timestamp.from(due), sourceType, sourceId
        );
    }
}