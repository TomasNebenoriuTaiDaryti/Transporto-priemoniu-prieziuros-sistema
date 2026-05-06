package com.example.back.Unit.reminder;

import com.example.back.reminder.dto.ReminderDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderDtosTest {

    @Test
    @DisplayName("ReminderResponse grazina visas reiksmes")
    void reminderResponseReturnsValues() {
        Instant dueAt = Instant.parse("2026-05-20T06:00:00Z");
        ReminderDtos.ReminderResponse response = new ReminderDtos.ReminderResponse(1L, "Draudimas", "Draudimas galioja iki 2026-05-20", dueAt, null, 30, 0L, "SCHEDULED");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Draudimas");
        assertThat(response.message()).isEqualTo("Draudimas galioja iki 2026-05-20");
        assertThat(response.dueAt()).isEqualTo(dueAt);
        assertThat(response.dueOdometerKm()).isNull();
        assertThat(response.notifyBeforeDays()).isEqualTo(30);
        assertThat(response.notifyBeforeKm()).isEqualTo(0L);
        assertThat(response.status()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("ReminderResponse metodai equals hashCode ir toString veikia")
    void reminderResponseRecordMethodsWork() {
        Instant dueAt = Instant.parse("2026-05-20T06:00:00Z");
        ReminderDtos.ReminderResponse first = new ReminderDtos.ReminderResponse(1L, "Draudimas", "Draudimas galioja iki 2026-05-20", dueAt, null, 30, 0L, "SCHEDULED");
        ReminderDtos.ReminderResponse second = new ReminderDtos.ReminderResponse(1L, "Draudimas", "Draudimas galioja iki 2026-05-20", dueAt, null, 30, 0L, "SCHEDULED");
        ReminderDtos.ReminderResponse different = new ReminderDtos.ReminderResponse(2L, "Tepalai", "Artėja: Tepalai", null, 150000L, 0, 3000L, "TRIGGERED");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
        assertThat(first.toString()).contains("Draudimas");
    }

    @Test
    @DisplayName("Dto klase turi privatu konstruktoriu")
    void privateConstructorWorks() throws Exception {
        Constructor<ReminderDtos> constructor = ReminderDtos.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        ReminderDtos dto = constructor.newInstance();

        assertThat(dto).isNotNull();
    }
}