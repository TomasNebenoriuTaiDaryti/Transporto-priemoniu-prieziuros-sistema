package com.example.back.Unit.reminder;

import com.example.back.reminder.service.ReminderPlanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderPlannerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ReminderPlanner planner = new ReminderPlanner(jdbc);

    @Test
    @DisplayName("Istrina priminimus pagal saltini")
    void deleteBySourceDeletesReminderBySource() {
        planner.deleteBySource(1L, "DOCUMENT", "doc-1");

        verify(jdbc).update(contains("delete from reminder"), eq(1L), eq("DOCUMENT"), eq("doc-1"));
        verify(jdbc).update(contains("source_type = ?"), eq(1L), eq("DOCUMENT"), eq("doc-1"));
        verify(jdbc).update(contains("source_id = ?"), eq(1L), eq("DOCUMENT"), eq("doc-1"));
    }

    @Test
    @DisplayName("Dokumento priminimas nieko nedaro kai data null")
    void upsertDocumentReminderDoesNothingWhenEndDateIsNull() {
        planner.upsertDocumentReminder(1L, "DOCUMENT", "doc-1", "Draudimas", null);

        verifyNoInteractions(jdbc);
    }

    @Test
    @DisplayName("Sukuria arba atnaujina dokumento priminima")
    void upsertDocumentReminderUpdatesJdbc() {
        LocalDate endDate = LocalDate.of(2026, 5, 20);

        planner.upsertDocumentReminder(1L, "DOCUMENT", "doc-1", "Draudimas", endDate);

        ArgumentCaptor<Timestamp> timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);
        verify(jdbc).update(
                contains("insert into reminder"),
                eq(1L),
                eq("Draudimas"),
                eq("Draudimas galioja iki 2026-05-20"),
                timestampCaptor.capture(),
                eq("DOCUMENT"),
                eq("doc-1")
        );

        Timestamp expected = Timestamp.from(endDate.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant());
        assertThat(timestampCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Ridos priminimas nieko nedaro kai rida null")
    void upsertMileageReminderDoesNothingWhenDueOdoKmIsNull() {
        planner.upsertMileageReminder(1L, "SERVICE_RECORD", "rec-1", "Tepalu keitimas", null);

        verifyNoInteractions(jdbc);
    }

    @Test
    @DisplayName("Sukuria arba atnaujina ridos priminima")
    void upsertMileageReminderUpdatesJdbc() {
        planner.upsertMileageReminder(1L, "SERVICE_RECORD", "rec-1", "Tepalu keitimas", 150000L);

        verify(jdbc).update(
                contains("insert into reminder"),
                eq(1L),
                eq("Tepalu keitimas"),
                eq("Artėja: Tepalu keitimas (liko apie 3000km)"),
                eq(150000L),
                eq("SERVICE_RECORD"),
                eq("rec-1")
        );
    }

    @Test
    @DisplayName("Datos priminimas nieko nedaro kai data null")
    void upsertDateReminderDoesNothingWhenDueDateIsNull() {
        planner.upsertDateReminder(1L, "SERVICE_RECORD", "rec-1", "Technine apziura", null);

        verifyNoInteractions(jdbc);
    }

    @Test
    @DisplayName("Sukuria arba atnaujina datos priminima")
    void upsertDateReminderUpdatesJdbc() {
        LocalDate dueDate = LocalDate.of(2026, 6, 15);

        planner.upsertDateReminder(1L, "SERVICE_RECORD", "rec-1", "Technine apziura", dueDate);

        ArgumentCaptor<Timestamp> timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);
        verify(jdbc).update(
                contains("insert into reminder"),
                eq(1L),
                eq("Technine apziura"),
                eq("Artėja: Technine apziura"),
                timestampCaptor.capture(),
                eq("SERVICE_RECORD"),
                eq("rec-1")
        );

        Timestamp expected = Timestamp.from(dueDate.atTime(9, 0).atZone(ZoneId.of("Europe/Vilnius")).toInstant());
        assertThat(timestampCaptor.getValue()).isEqualTo(expected);
    }
}