package com.example.back.Unit.reminder;

import com.example.back.reminder.api.ReminderController;
import com.example.back.reminder.dto.ReminderDtos;
import com.example.back.reminder.service.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReminderControllerTest {

    private final ReminderService reminderService = mock(ReminderService.class);
    private final ReminderController controller = new ReminderController(reminderService);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Grazina priminimu sarasa is serviso")
    void listReturnsRemindersFromService() {
        List<ReminderDtos.ReminderResponse> reminders = List.of(
                new ReminderDtos.ReminderResponse(1L, "Draudimas", "Draudimas galioja iki 2026-05-20", Instant.parse("2026-05-20T06:00:00Z"), null, 30, 0L, "SCHEDULED"),
                new ReminderDtos.ReminderResponse(2L, "Tepalai", "Artėja: Tepalai (liko apie 3000km)", null, 150000L, 0, 3000L, "TRIGGERED")
        );

        when(reminderService.list(10L, auth)).thenReturn(reminders);
        List<ReminderDtos.ReminderResponse> result = controller.list(10L, auth);
        assertThat(result).isEqualTo(reminders);
        verify(reminderService).list(10L, auth);
        verifyNoMoreInteractions(reminderService);
    }

    @Test
    @DisplayName("Kai servise ivyksta klaida, ji perduodama toliau")
    void listPropagatesServiceException() {
        when(reminderService.list(10L, auth)).thenThrow(new IllegalArgumentException("Mašina nerasta"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.list(10L, auth)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
        verify(reminderService).list(10L, auth);
        verifyNoMoreInteractions(reminderService);
    }
}