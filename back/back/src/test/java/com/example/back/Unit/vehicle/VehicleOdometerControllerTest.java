package com.example.back.Unit.vehicle;

import com.example.back.security.AuthUser;
import com.example.back.vehicle.api.VehicleOdometerController;
import com.example.back.vehicle.dto.OdometerAppendRequest;
import com.example.back.vehicle.service.VehicleOdometerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;

class VehicleOdometerControllerTest {

    private final VehicleOdometerService service = mock(VehicleOdometerService.class);
    private final VehicleOdometerController controller = new VehicleOdometerController(service);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Ridos pridėjimas perduoda vartotojo ir mašinos ID servisui")
    void appendDelegatesToService() {
        OdometerAppendRequest request = new OdometerAppendRequest(50L);

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.append(1L, request, auth);

            verify(service).appendKmIfActiveUser(10L, 1L, 50L);
        }
    }
}