package com.example.back.Unit.reminder;

import com.example.back.reminder.api.ReminderController;
import com.example.back.security.AuthUser;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final VehicleAccessService access = mock(VehicleAccessService.class);
    private final ReminderController controller = new ReminderController(jdbc, access);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Savininkui grazina priminimu sarasa")
    void listReturnsRemindersForOwner() {
        Vehicle vehicle = new Vehicle(10L);
        vehicle.setId(1L);

        List<Map<String, Object>> reminders = List.of(
                Map.of("id", 1L, "title", "Technine", "status", "SCHEDULED"),
                Map.of("id", 2L, "title", "Draudimas", "status", "TRIGGERED")
        );

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
            when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);
            when(jdbc.queryForList(anyString(), eq(1L))).thenReturn(reminders);

            List<Map<String, Object>> result = controller.list(1L, auth);

            assertThat(result).isEqualTo(reminders);
            verify(access).getVehicleOrThrow(1L);
            verify(access).isVehicleOwner(10L, vehicle);
            verify(jdbc).queryForList(contains("from reminder"), eq(1L));
            verify(jdbc).queryForList(contains("status in ('SCHEDULED','TRIGGERED')"), eq(1L));
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

            List<Map<String, Object>> result = controller.list(1L, auth);

            assertThat(result).isEmpty();
            verify(access).getVehicleOrThrow(1L);
            verify(access).isVehicleOwner(10L, vehicle);
            verifyNoInteractions(jdbc);
        }
    }

    @Test
    @DisplayName("Kai masina nerasta klaida perduodama toliau")
    void listPropagatesExceptionWhenVehicleIsMissing() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(access.getVehicleOrThrow(1L)).thenThrow(new IllegalArgumentException("Masina nerasta"));

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.list(1L, auth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Masina nerasta");

            verify(access).getVehicleOrThrow(1L);
            verify(access, never()).isVehicleOwner(anyLong(), any());
            verifyNoInteractions(jdbc);
        }
    }
}