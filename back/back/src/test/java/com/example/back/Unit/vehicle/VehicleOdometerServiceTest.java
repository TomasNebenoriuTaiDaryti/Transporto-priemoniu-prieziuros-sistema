package com.example.back.Unit.vehicle;

import com.example.back.vehicle.domain.ActiveVehicle;
import com.example.back.vehicle.repo.ActiveVehicleRepo;
import com.example.back.vehicle.repo.VehicleRepo;
import com.example.back.vehicle.service.VehicleOdometerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleOdometerServiceTest {

    private final ActiveVehicleRepo activeVehicles = mock(ActiveVehicleRepo.class);
    private final VehicleRepo vehicles = mock(VehicleRepo.class);
    private final VehicleOdometerService service = new VehicleOdometerService(activeVehicles, vehicles);

    @Test
    @DisplayName("Null ridos pokytis nieko nedaro")
    void nullDeltaDoesNothing() {
        service.appendKmIfActiveUser(10L, 1L, null);

        verifyNoInteractions(activeVehicles, vehicles);
    }

    @Test
    @DisplayName("Nulinis ridos pokytis nieko nedaro")
    void zeroDeltaDoesNothing() {
        service.appendKmIfActiveUser(10L, 1L, 0L);

        verifyNoInteractions(activeVehicles, vehicles);
    }

    @Test
    @DisplayName("Neigiamas ridos pokytis nieko nedaro")
    void negativeDeltaDoesNothing() {
        service.appendKmIfActiveUser(10L, 1L, -5L);

        verifyNoInteractions(activeVehicles, vehicles);
    }

    @Test
    @DisplayName("Rida pridedama kai mašiną naudoja tas pats vartotojas")
    void appendsKmWhenActiveUserMatches() {
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.of(new ActiveVehicle(10L, 1L)));
        when(vehicles.addOdometerKm(1L, 50L)).thenReturn(1);

        service.appendKmIfActiveUser(10L, 1L, 50L);

        verify(vehicles).addOdometerKm(1L, 50L);
    }

    @Test
    @DisplayName("Klaida kai mašina nėra naudojama")
    void throwsWhenVehicleIsNotActive() {
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.appendKmIfActiveUser(10L, 1L, 50L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Ši mašina šiuo metu nenaudojama");

        verify(vehicles, never()).addOdometerKm(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Klaida kai mašiną naudoja kitas vartotojas")
    void throwsWhenVehicleUsedByOtherUser() {
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.of(new ActiveVehicle(99L, 1L)));

        assertThatThrownBy(() -> service.appendKmIfActiveUser(10L, 1L, 50L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Negali keisti ridos: šią mašiną naudoja kitas vartotojas");

        verify(vehicles, never()).addOdometerKm(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Klaida kai ridos atnaujinimas nepakeičia įrašo")
    void throwsWhenVehicleUpdateReturnsZero() {
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.of(new ActiveVehicle(10L, 1L)));
        when(vehicles.addOdometerKm(1L, 50L)).thenReturn(0);

        assertThatThrownBy(() -> service.appendKmIfActiveUser(10L, 1L, 50L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }
}