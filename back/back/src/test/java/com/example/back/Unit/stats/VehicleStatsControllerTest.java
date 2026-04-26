package com.example.back.Unit.stats;

import com.example.back.security.AuthUser;
import com.example.back.stats.api.VehicleStatsController;
import com.example.back.stats.dto.VehicleStatsResponse;
import com.example.back.stats.service.VehicleStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VehicleStatsControllerTest {

    private final VehicleStatsService service = mock(VehicleStatsService.class);
    private final VehicleStatsController controller = new VehicleStatsController(service);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Mašinos statistika grąžinama pagal prisijungusį vartotoją")
    void vehicleStatsReturnsServiceResponseForAuthenticatedUser() {
        VehicleStatsResponse response = new VehicleStatsResponse(1L, "Volkswagen Golf (2000)", List.of(), List.of(), List.of(), List.of());

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.stats(10L, 1L)).thenReturn(response);

            VehicleStatsResponse result = controller.vehicleStats(1L, auth);

            assertThat(result).isEqualTo(response);
            verify(service).stats(10L, 1L);
        }
    }
}