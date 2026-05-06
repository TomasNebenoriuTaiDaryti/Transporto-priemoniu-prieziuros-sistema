package com.example.back.Unit.reminder;

import com.example.back.reminder.domain.Reminder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderDomainTest {

    @Test
    @DisplayName("Tuscias konstruktorius sukuria pagal pradines reiksmes")
    void protectedConstructorCreatesJpaEntityWithDefaults() throws Exception {
        Constructor<Reminder> constructor = Reminder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Reminder reminder = constructor.newInstance();

        assertThat(reminder.getId()).isNull();
        assertThat(reminder.getVehicleId()).isNull();
        assertThat(reminder.getTitle()).isNull();
        assertThat(reminder.getMessage()).isNull();
        assertThat(reminder.getDueAt()).isNull();
        assertThat(reminder.getDueOdometerKm()).isNull();
        assertThat(reminder.getNotifyBeforeDays()).isEqualTo(7);
        assertThat(reminder.getNotifyBeforeKm()).isEqualTo(300L);
        assertThat(reminder.getStatus()).isEqualTo("SCHEDULED");
        assertThat(reminder.getCreatedAt()).isNotNull();
        assertThat(reminder.getSourceType()).isNull();
        assertThat(reminder.getSourceId()).isNull();
    }

    @Test
    @DisplayName("Konstruktorius sukuria priminima pagal data")
    void constructorCreatesDateReminder() {
        Instant dueAt = Instant.parse("2026-05-20T06:00:00Z");
        Reminder reminder = new Reminder(1L, "Draudimas", "Draudimas galioja iki 2026-05-20", dueAt, null, 30, 0L, "DOCUMENT", "doc-1");

        assertThat(reminder.getId()).isNull();
        assertThat(reminder.getVehicleId()).isEqualTo(1L);
        assertThat(reminder.getTitle()).isEqualTo("Draudimas");
        assertThat(reminder.getMessage()).isEqualTo("Draudimas galioja iki 2026-05-20");
        assertThat(reminder.getDueAt()).isEqualTo(dueAt);
        assertThat(reminder.getDueOdometerKm()).isNull();
        assertThat(reminder.getNotifyBeforeDays()).isEqualTo(30);
        assertThat(reminder.getNotifyBeforeKm()).isEqualTo(0L);
        assertThat(reminder.getStatus()).isEqualTo("SCHEDULED");
        assertThat(reminder.getCreatedAt()).isNotNull();
        assertThat(reminder.getSourceType()).isEqualTo("DOCUMENT");
        assertThat(reminder.getSourceId()).isEqualTo("doc-1");
    }

    @Test
    @DisplayName("updateDateReminder atnaujina datos priminima")
    void updateDateReminderUpdatesDateFieldsAndClearsMileageDue() {
        Reminder reminder = new Reminder(1L, "Senas", "Sena zinute", null, 120000L, 0, 3000L, "DOCUMENT", "doc-1");
        Instant dueAt = Instant.parse("2026-05-20T06:00:00Z");
        reminder.updateDateReminder("Draudimas", "Draudimas galioja iki 2026-05-20", dueAt, 30, 0L);

        assertThat(reminder.getTitle()).isEqualTo("Draudimas");
        assertThat(reminder.getMessage()).isEqualTo("Draudimas galioja iki 2026-05-20");
        assertThat(reminder.getDueAt()).isEqualTo(dueAt);
        assertThat(reminder.getDueOdometerKm()).isNull();
        assertThat(reminder.getNotifyBeforeDays()).isEqualTo(30);
        assertThat(reminder.getNotifyBeforeKm()).isEqualTo(0L);
        assertThat(reminder.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("updateMileageReminder atnaujina ridos priminima")
    void updateMileageReminderUpdatesMileageFieldsAndClearsDateDue() {
        Reminder reminder = new Reminder(1L, "Senas", "Sena zinute", Instant.parse("2026-01-01T07:00:00Z"), null, 30, 0L, "RECORD_OIL_KM", "rec-1");
        reminder.updateMileageReminder("Tepalai", "Artėja: Tepalai (liko apie 3000km)", 150000L, 0, 3000L);

        assertThat(reminder.getTitle()).isEqualTo("Tepalai");
        assertThat(reminder.getMessage()).isEqualTo("Artėja: Tepalai (liko apie 3000km)");
        assertThat(reminder.getDueAt()).isNull();
        assertThat(reminder.getDueOdometerKm()).isEqualTo(150000L);
        assertThat(reminder.getNotifyBeforeDays()).isEqualTo(0);
        assertThat(reminder.getNotifyBeforeKm()).isEqualTo(3000L);
        assertThat(reminder.getStatus()).isEqualTo("SCHEDULED");
    }
}