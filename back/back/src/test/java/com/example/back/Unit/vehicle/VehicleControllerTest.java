package com.example.back.Unit.vehicle;

import com.example.back.security.AuthUser;
import com.example.back.vehicle.api.VehicleController;
import com.example.back.vehicle.dto.*;
import com.example.back.vehicle.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VehicleControllerTest {

    private final VehicleService service = mock(VehicleService.class);
    private final VehicleController controller = new VehicleController(service);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Peržiūra pagal VIN grąžina serviso atsakymą")
    void previewReturnsServiceResponse() {
        VehiclePreviewRequest request = new VehiclePreviewRequest("WVWZZZ1JZXW000001");
        VehiclePreviewResponse response = new VehiclePreviewResponse("WVWZZZ1JZXW000001", "VW", "Golf", 2000, 1900, "Manual", "Diesel", "DIESEL", "FWD", "Hatchback", 5, 5, 140.0, "Germany", "Volkswagen");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.previewByVin(10L, request.vin())).thenReturn(response);

            VehiclePreviewResponse result = controller.preview(request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).previewByVin(10L, "WVWZZZ1JZXW000001");
        }
    }

    @Test
    @DisplayName("Asmeninės mašinos sukūrimas perduoda vartotojo ID servisui")
    void createPersonalDelegatesToService() {
        VehicleConfirmRequest request = new VehicleConfirmRequest("WVWZZZ1JZXW000001", "VW", "Golf", 2000, 1900, "Manual", 1000L, "DIESEL", "FWD", "Hatchback", 5, 5, null, "Germany", "Volkswagen");
        VehicleResponse response = new VehicleResponse(1L, request.vin(), "VW", "Golf", 2000, "DIESEL", 1000L, null);

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.confirmCreatePersonal(10L, request)).thenReturn(response);

            VehicleResponse result = controller.createPersonal(request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).confirmCreatePersonal(10L, request);
        }
    }

    @Test
    @DisplayName("Pasiekiamų mašinų sąrašas grąžinamas iš serviso")
    void accessibleReturnsServiceResponse() {
        List<VehicleListItemResponse> response = List.of(new VehicleListItemResponse(1L, "VIN", "VW", "Golf", 2000, "DIESEL", 1000L, null, 10L, "owner@test.com", true, true, false, null));

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.listAccessibleVehicles(10L)).thenReturn(response);

            List<VehicleListItemResponse> result = controller.accessible(auth);

            assertThat(result).isEqualTo(response);
            verify(service).listAccessibleVehicles(10L);
        }
    }

    @Test
    @DisplayName("Mano mašinų sąrašas grąžinamas iš serviso")
    void myReturnsServiceResponse() {
        List<VehicleResponse> response = List.of(new VehicleResponse(1L, "VIN", "VW", "Golf", 2000, "DIESEL", 1000L, null));

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.listMyVehicles(10L)).thenReturn(response);

            List<VehicleResponse> result = controller.my(auth);

            assertThat(result).isEqualTo(response);
            verify(service).listMyVehicles(10L);
        }
    }

    @Test
    @DisplayName("Mašinos detalės grąžinamos pagal ID")
    void detailsReturnsServiceResponse() {
        VehicleDetailsResponse response = new VehicleDetailsResponse(1L, "VIN", "VW", "Golf", 2000, 1900, "DIESEL", 1000L, "Manual", "FWD", "Hatchback", 5, 5, null, "Germany", "Volkswagen", null, true);

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.getVehicleDetailsForViewer(10L, 1L)).thenReturn(response);

            VehicleDetailsResponse result = controller.details(1L, auth);

            assertThat(result).isEqualTo(response);
            verify(service).getVehicleDetailsForViewer(10L, 1L);
        }
    }

    @Test
    @DisplayName("Mašinos atnaujinimas perduoda duomenis servisui")
    void updateDelegatesToService() {
        VehicleUpdateRequest request = new VehicleUpdateRequest("VW", "Golf", 2001, 1900, "DIESEL", 2000L, "Manual", "FWD", "Hatchback", 5, 5, 140.0, "Germany", "Volkswagen");
        VehicleResponse response = new VehicleResponse(1L, "VIN", "VW", "Golf", 2001, "DIESEL", 2000L, null);

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.updateMyVehicle(10L, 1L, request)).thenReturn(response);

            VehicleResponse result = controller.update(1L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).updateMyVehicle(10L, 1L, request);
        }
    }

    @Test
    @DisplayName("Mašinos trynimas iškviečia servisą")
    void deleteDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.delete(1L, auth);

            verify(service).deleteMyVehicle(10L, 1L);
        }
    }

    @Test
    @DisplayName("Grupės mašinos grąžinamos pagal grupės ID")
    void groupVehiclesReturnsServiceResponse() {
        List<VehicleResponse> response = List.of(new VehicleResponse(1L, "VIN", "VW", "Golf", 2000, "DIESEL", 1000L, 5L));
        when(service.listGroupVehicles(5L)).thenReturn(response);

        List<VehicleResponse> result = controller.groupVehicles(5L);

        assertThat(result).isEqualTo(response);
        verify(service).listGroupVehicles(5L);
    }

    @Test
    @DisplayName("Mašinos naudojimas iškviečia aktyvavimo servisą")
    void useDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.use(1L, auth);

            verify(service).setActiveVehicle(10L, 1L);
        }
    }

    @Test
    @DisplayName("Mašinos nebenaudojimas iškviečia išvalymo servisą")
    void unuseDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.unuse(auth);

            verify(service).clearActiveVehicle(10L);
        }
    }
}