package com.example.back.reminder.dto;

import java.time.Instant;

public final class ReminderDtos {

    private ReminderDtos() {}

    public record ReminderResponse(
            Long id,
            String title,
            String message,
            Instant dueAt,
            Long dueOdometerKm,
            Integer notifyBeforeDays,
            Long notifyBeforeKm,
            String status
    ) {}
}