package com.example.back.Unit.vehicle;

import com.example.back.group.repo.GroupMemberRepo;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.repo.VehicleRepo;
import com.example.back.vehicle.service.VehicleAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleAccessServiceTest {

    private final VehicleRepo vehicles = mock(VehicleRepo.class);
    private final GroupMemberRepo groupMembers = mock(GroupMemberRepo.class);
    private final VehicleAccessService service = new VehicleAccessService(vehicles, groupMembers);

    @Test
    @DisplayName("Mašina grąžinama, kai ji randama")
    void getVehicleReturnsVehicle() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));

        Vehicle result = service.getVehicleOrThrow(1L);

        assertThat(result).isSameAs(vehicle);
    }

    @Test
    @DisplayName("Klaida, kai mašina nerasta")
    void getVehicleThrowsWhenMissing() {
        when(vehicles.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVehicleOrThrow(1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Savininkas gali matyti mašiną")
    void ownerCanViewVehicle() {
        Vehicle vehicle = vehicle(1L, 10L, null);

        boolean result = service.canView(10L, vehicle);

        assertThat(result).isTrue();
        verifyNoInteractions(groupMembers);
    }

    @Test
    @DisplayName("Ne savininkas negali matyti mašinos be grupės")
    void nonOwnerCannotViewVehicleWithoutGroup() {
        Vehicle vehicle = vehicle(1L, 10L, null);

        boolean result = service.canView(99L, vehicle);

        assertThat(result).isFalse();
        verifyNoInteractions(groupMembers);
    }

    @Test
    @DisplayName("Grupės narys gali matyti grupės mašiną")
    void groupMemberCanViewVehicle() {
        Vehicle vehicle = vehicle(1L, 10L, 5L);
        when(groupMembers.existsByGroupIdAndUserId(5L, 99L)).thenReturn(true);

        boolean result = service.canView(99L, vehicle);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Grupės ne narys negali matyti grupės mašinos")
    void nonGroupMemberCannotViewGroupVehicle() {
        Vehicle vehicle = vehicle(1L, 10L, 5L);
        when(groupMembers.existsByGroupIdAndUserId(5L, 99L)).thenReturn(false);

        boolean result = service.canView(99L, vehicle);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Patikrinama, ar vartotojas yra savininkas")
    void checksVehicleOwner() {
        Vehicle vehicle = vehicle(1L, 10L, null);

        assertThat(service.isVehicleOwner(10L, vehicle)).isTrue();
        assertThat(service.isVehicleOwner(99L, vehicle)).isFalse();
    }

    private static Vehicle vehicle(Long id, Long ownerUserId, Long groupId) {
        Vehicle vehicle = new Vehicle(ownerUserId);
        vehicle.setId(id);
        vehicle.setGroupId(groupId);
        return vehicle;
    }
}