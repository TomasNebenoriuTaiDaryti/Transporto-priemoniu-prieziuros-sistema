package com.example.back.reminder.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reminder")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 600)
    private String message;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "due_odometer_km")
    private Long dueOdometerKm;

    @Column(name = "notify_before_days", nullable = false)
    private Integer notifyBeforeDays = 7;

    @Column(name = "notify_before_km", nullable = false)
    private Long notifyBeforeKm = 300L;

    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 80)
    private String sourceId;

    protected Reminder() {}

    public Reminder(Long vehicleId, String title, String message, Instant dueAt, Long dueOdometerKm, Integer notifyBeforeDays, Long notifyBeforeKm, String sourceType, String sourceId) {
        this.vehicleId = vehicleId;
        this.title = title;
        this.message = message;
        this.dueAt = dueAt;
        this.dueOdometerKm = dueOdometerKm;
        this.notifyBeforeDays = notifyBeforeDays;
        this.notifyBeforeKm = notifyBeforeKm;
        this.status = "SCHEDULED";
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    public Long getId() { return id; }
    public Long getVehicleId() { return vehicleId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getDueAt() { return dueAt; }
    public Long getDueOdometerKm() { return dueOdometerKm; }
    public Integer getNotifyBeforeDays() { return notifyBeforeDays; }
    public Long getNotifyBeforeKm() { return notifyBeforeKm; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }

    public void updateDateReminder(String title, String message, Instant dueAt, Integer notifyBeforeDays, Long notifyBeforeKm) {
        this.title = title;
        this.message = message;
        this.dueAt = dueAt;
        this.dueOdometerKm = null;
        this.notifyBeforeDays = notifyBeforeDays;
        this.notifyBeforeKm = notifyBeforeKm;
        this.status = "SCHEDULED";
    }

    public void updateMileageReminder(String title, String message, Long dueOdometerKm, Integer notifyBeforeDays, Long notifyBeforeKm) {
        this.title = title;
        this.message = message;
        this.dueAt = null;
        this.dueOdometerKm = dueOdometerKm;
        this.notifyBeforeDays = notifyBeforeDays;
        this.notifyBeforeKm = notifyBeforeKm;
        this.status = "SCHEDULED";
    }
}