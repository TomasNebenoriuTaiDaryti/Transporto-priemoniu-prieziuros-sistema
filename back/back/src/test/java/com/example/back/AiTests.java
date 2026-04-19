package com.example.back;

import com.example.back.ai.service.VehicleAiContextService;
import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.record.repo.ServiceRecordRepo;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AiTests {

    @Test
    @DisplayName("SGR-2: DI kontekste neturi būti asmens duomenų ir VIN")
    void aiContextShouldNotContainPersonalDataOrVin() {
        VehicleAccessService vehicleAccessService = mock(VehicleAccessService.class);
        ServiceRecordRepo recordRepo = mock(ServiceRecordRepo.class);
        VehicleDocumentRepo documentRepo = mock(VehicleDocumentRepo.class);
        ObjectMapper objectMapper = new ObjectMapper();

        VehicleAiContextService service = new VehicleAiContextService(vehicleAccessService, recordRepo, documentRepo, objectMapper);
        Long userId = 1L;
        Long vehicleId = 10L;
        String userEmail = "test1@test.com";
        String userName = "Tomas";
        String userSurname = "Testinis";
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setVin("WVWZZZ1JZ3B044671");
        vehicle.setMake("Volkswagen");
        vehicle.setModel("Golf");
        vehicle.setModelYear(2015);
        vehicle.setOdometerKm(185000L);

        when(vehicleAccessService.getVehicleOrThrow(vehicleId)).thenReturn(vehicle);
        when(vehicleAccessService.canView(userId, vehicle)).thenReturn(true);
        when(recordRepo.findAllByVehicleIdOrderByPerformedAtDesc(vehicleId)).thenReturn(List.of());
        when(documentRepo.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId)).thenReturn(List.of());

        String context = service.buildSanitizedContext(userId, vehicleId);

        assertNotNull(context);
        assertTrue(context.contains("AUTOMOBILIO KONTEKSTAS"));
        assertTrue(context.contains("Volkswagen"));
        assertTrue(context.contains("Golf"));

        assertFalse(context.contains("WVWZZZ1JZ3B044671"));
        assertFalse(context.contains(userEmail));
        assertFalse(context.contains(userName));
        assertFalse(context.contains(userSurname));
        assertFalse(context.toLowerCase().contains("email"));
        assertFalse(context.toLowerCase().contains("el. pašt"));
        assertFalse(context.toLowerCase().contains("vard"));
    }
}