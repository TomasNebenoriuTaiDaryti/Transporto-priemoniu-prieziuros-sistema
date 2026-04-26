package com.example.back.Unit.vehicle;

import com.example.back.auth.repo.AccountRepo;
import com.example.back.group.repo.GroupMemberRepo;
import com.example.back.vehicle.domain.ActiveVehicle;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.dto.*;
import com.example.back.vehicle.repo.ActiveVehicleRepo;
import com.example.back.vehicle.repo.VehicleRepo;
import com.example.back.vehicle.service.VehicleService;
import com.example.back.vehicle.vin.VincarioClient;
import com.example.back.vehicle.vin.dto.VincarioDecodeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VehicleServiceTest {

    private final VehicleRepo vehicles = mock(VehicleRepo.class);
    private final VincarioClient vincario = mock(VincarioClient.class);
    private final GroupMemberRepo groupMembers = mock(GroupMemberRepo.class);
    private final ActiveVehicleRepo activeVehicles = mock(ActiveVehicleRepo.class);
    private final AccountRepo users = mock(AccountRepo.class);
    private final VehicleService service = new VehicleService(vehicles, vincario, groupMembers, activeVehicles, users);

    @Test
    @DisplayName("Normalizuoja VIN ir grąžina dekoduotus duomenis")
    void previewByVinNormalizesVinAndReturnsDecodedData() {
        VincarioDecodeResponse decoded = decodedVehicle();
        when(vehicles.existsByOwnerUserIdAndVin(10L, "WVWZZZ1JZXW000001")).thenReturn(false);
        when(vincario.decode("WVWZZZ1JZXW000001")).thenReturn(decoded);

        VehiclePreviewResponse result = service.previewByVin(10L, "wvwzzz1jzxw000001");

        assertThat(result.vin()).isEqualTo("WVWZZZ1JZXW000001");
        assertThat(result.make()).isEqualTo("Volkswagen");
        assertThat(result.model()).isEqualTo("Golf");
        assertThat(result.modelYear()).isEqualTo(2000);
        assertThat(result.engineDisplacementCc()).isEqualTo(1900);
        assertThat(result.transmission()).isEqualTo("Manual");
        assertThat(result.fuelTypeRaw()).isEqualTo("Diesel");
        assertThat(result.fuelType()).isEqualTo("DIESEL");
        assertThat(result.drive()).isEqualTo("FWD");
        assertThat(result.body()).isEqualTo("Hatchback");
        assertThat(result.doors()).isEqualTo(5);
        assertThat(result.seats()).isEqualTo(5);
        assertThat(result.co2Gkm()).isEqualTo(140.0);
        assertThat(result.plantCountry()).isEqualTo("Germany");
        assertThat(result.manufacturer()).isEqualTo("Volkswagen AG");
    }

    @Test
    @DisplayName("VIN peržiūra meta klaidą, kai VIN jau yra garaže")
    void previewByVinThrowsWhenDuplicateVinExists() {
        when(vehicles.existsByOwnerUserIdAndVin(10L, "WVWZZZ1JZXW000001")).thenReturn(true);
        assertThatThrownBy(() -> service.previewByVin(10L, "WVWZZZ1JZXW000001")).isInstanceOf(IllegalArgumentException.class).hasMessage("Šis VIN jau naudojamas tavo garaže");
        verifyNoInteractions(vincario);
    }

    @Test
    @DisplayName("VIN peržiūra meta klaidą, kai VIN ilgis neteisingas")
    void previewByVinThrowsWhenVinLengthInvalid() {
        assertThatThrownBy(() -> service.previewByVin(10L, "ABC")).isInstanceOf(IllegalArgumentException.class).hasMessage("VIN turi būti 17 simbolių");
        verifyNoInteractions(vincario);
    }

    @Test
    @DisplayName("Asmeninės mašinos sukūrimas išsaugo užpildytą entity")
    void confirmCreatePersonalSavesVehicle() {
        VehicleConfirmRequest request = confirmRequest("wvwzzz1jzxw000001");
        when(vehicles.existsByOwnerUserIdAndVin(10L, "WVWZZZ1JZXW000001")).thenReturn(false);
        when(vehicles.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(1L);
            return vehicle;
        });

        VehicleResponse result = service.confirmCreatePersonal(10L, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.vin()).isEqualTo("WVWZZZ1JZXW000001");
        assertThat(result.make()).isEqualTo("Volkswagen");
        assertThat(result.model()).isEqualTo("Golf");
        assertThat(result.modelYear()).isEqualTo(2000);
        assertThat(result.fuelType()).isEqualTo("DIESEL");
        assertThat(result.odometerKm()).isEqualTo(1000L);
        assertThat(result.groupId()).isNull();
        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicles).save(captor.capture());
        assertThat(captor.getValue().getOwnerUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getEngineDisplacementCc()).isEqualTo(1900);
        assertThat(captor.getValue().getCo2Gkm()).isEqualTo(new BigDecimal("140.50"));
    }

    @Test
    @DisplayName("Asmeninės mašinos sukūrimas leidžia null VIN")
    void confirmCreatePersonalAllowsNullVin() {
        VehicleConfirmRequest request = confirmRequest(null);
        when(vehicles.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(1L);
            return vehicle;
        });

        VehicleResponse result = service.confirmCreatePersonal(10L, request);
        assertThat(result.vin()).isNull();
        verify(vehicles, never()).existsByOwnerUserIdAndVin(anyLong(), anyString());
    }

    @Test
    @DisplayName("Asmeninės mašinos sukūrimas leidžia tuščią VIN")
    void confirmCreatePersonalAllowsBlankVin() {
        VehicleConfirmRequest request = confirmRequest("   ");
        when(vehicles.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(1L);
            return vehicle;
        });

        VehicleResponse result = service.confirmCreatePersonal(10L, request);
        assertThat(result.vin()).isNull();
        verify(vehicles, never()).existsByOwnerUserIdAndVin(anyLong(), anyString());
    }

    @Test
    @DisplayName("Asmeninės mašinos sukūrimas meta klaidą, kai VIN ilgis neteisingas")
    void confirmCreatePersonalThrowsWhenVinLengthInvalid() {
        VehicleConfirmRequest request = confirmRequest("ABC");
        assertThatThrownBy(() -> service.confirmCreatePersonal(10L, request)).isInstanceOf(IllegalArgumentException.class).hasMessage("VIN turi būti 17 simbolių");
    }

    @Test
    @DisplayName("Asmeninės mašinos sukūrimas meta klaidą, kai VIN dubliuojasi")
    void confirmCreatePersonalThrowsWhenDuplicateVinExists() {
        VehicleConfirmRequest request = confirmRequest("WVWZZZ1JZXW000001");
        when(vehicles.existsByOwnerUserIdAndVin(10L, "WVWZZZ1JZXW000001")).thenReturn(true);
        assertThatThrownBy(() -> service.confirmCreatePersonal(10L, request)).isInstanceOf(IllegalArgumentException.class).hasMessage("Šis VIN jau naudojamas tavo garaže");
        verify(vehicles, never()).save(any());
    }

    @Test
    @DisplayName("Mano mašinų sąrašas sumapinamas į response")
    void listMyVehiclesMapsToResponse() {
        when(vehicles.findAllByOwnerUserId(10L)).thenReturn(List.of(vehicle(1L, 10L, null)));

        List<VehicleResponse> result = service.listMyVehicles(10L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).make()).isEqualTo("Volkswagen");
    }

    @Test
    @DisplayName("Mašinos atnaujinimas pakeičia laukus")
    void updateMyVehicleUpdatesFields() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        VehicleUpdateRequest request = updateRequest();
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));

        VehicleResponse result = service.updateMyVehicle(10L, 1L, request);
        assertThat(result.modelYear()).isEqualTo(2001);
        assertThat(vehicle.getEngineDisplacementCc()).isEqualTo(2000);
        assertThat(vehicle.getCo2Gkm()).isEqualTo(BigDecimal.valueOf(150.5));
        assertThat(vehicle.getManufacturer()).isEqualTo("VW AG");
    }

    @Test
    @DisplayName("Mašinos atnaujinimas meta klaidą, kai mašina nerasta")
    void updateMyVehicleThrowsWhenMissing() {
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateMyVehicle(10L, 1L, updateRequest())).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Mašinos trynimas ištrina tik savininko mašiną be grupės")
    void deleteMyVehicleDeletesOwnedVehicleWithoutGroup() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.of(new ActiveVehicle(99L, 1L)));
        service.deleteMyVehicle(10L, 1L);
        verify(activeVehicles).deleteById(99L);
        verify(vehicles).delete(vehicle);
    }

    @Test
    @DisplayName("Mašinos trynimas be aktyvaus įrašo neištrina aktyvios mašinos")
    void deleteMyVehicleWithoutActiveVehicleDoesNotDeleteActiveVehicle() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.empty());
        service.deleteMyVehicle(10L, 1L);
        verify(activeVehicles, never()).deleteById(anyLong());
        verify(vehicles).delete(vehicle);
    }

    @Test
    @DisplayName("Mašinos trynimas neištrina, kai mašina grupėje")
    void deleteMyVehicleThrowsWhenVehicleIsInGroup() {
        Vehicle vehicle = vehicle(1L, 10L, 5L);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.deleteMyVehicle(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Negalima ištrinti, kol mašina yra grupėje. Pirma pašalink iš grupės.");

        verify(vehicles, never()).delete(any());
    }

    @Test
    @DisplayName("Mašinos trynimas meta klaidą, kai mašina nerasta")
    void deleteMyVehicleThrowsWhenMissing() {
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteMyVehicle(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Mašina priskiriama grupei")
    void assignToGroupSetsGroupId() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        VehicleResponse result = service.assignToGroup(10L, 1L, 5L);

        assertThat(result.groupId()).isEqualTo(5L);
        assertThat(vehicle.getGroupId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Mašinos priskyrimas grupei meta klaidą, kai jau priskirta")
    void assignToGroupThrowsWhenAlreadyAssigned() {
        Vehicle vehicle = vehicle(1L, 10L, 5L);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        assertThatThrownBy(() -> service.assignToGroup(10L, 1L, 6L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Ši mašina jau priskirta grupei. Pirma pašalink iš grupės.");
    }

    @Test
    @DisplayName("Mašinos priskyrimas grupei meta klaidą, kai mašina nerasta")
    void assignToGroupThrowsWhenMissing() {
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assignToGroup(10L, 1L, 5L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Mašina pašalinama iš grupės")
    void removeFromGroupClearsGroupId() {
        Vehicle vehicle = vehicle(1L, 10L, 5L);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        VehicleResponse result = service.removeFromGroup(10L, 1L, 5L);

        assertThat(result.groupId()).isNull();
        assertThat(vehicle.getGroupId()).isNull();
    }

    @Test
    @DisplayName("Mašinos pašalinimas iš grupės meta klaidą, kai grupė nesutampa")
    void removeFromGroupThrowsWhenGroupDoesNotMatch() {
        Vehicle vehicle = vehicle(1L, 10L, 6L);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        assertThatThrownBy(() -> service.removeFromGroup(10L, 1L, 5L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nėra šioje grupėje");
    }

    @Test
    @DisplayName("Pašalinimas iš grupės meta klaidą, kai mašina neturi grupės")
    void removeFromGroupThrowsWhenVehicleHasNoGroup() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.of(vehicle));
        assertThatThrownBy(() -> service.removeFromGroup(10L, 1L, 5L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nėra šioje grupėje");
    }

    @Test
    @DisplayName("Mašinos pašalinimas iš grupės meta klaidą, kai mašina nerasta")
    void removeFromGroupThrowsWhenMissing() {
        when(vehicles.findByIdAndOwnerUserId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeFromGroup(10L, 1L, 5L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Grupės mašinų sąrašas sumapinamas į response")
    void listGroupVehiclesMapsToResponse() {
        when(vehicles.findAllByGroupId(5L)).thenReturn(List.of(vehicle(1L, 10L, 5L)));
        List<VehicleResponse> result = service.listGroupVehicles(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).groupId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Pasiekiamų mašinų sąrašas įtraukia savas mašinas")
    void listAccessibleVehiclesIncludesOwnedVehicles() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findAllByOwnerUserId(10L)).thenReturn(List.of(vehicle));
        when(groupMembers.findAllByUserId(10L)).thenReturn(List.of());
        when(activeVehicles.findByUserId(10L)).thenReturn(Optional.of(new ActiveVehicle(10L, 1L)));
        when(activeVehicles.findAll()).thenReturn(List.of(new ActiveVehicle(10L, 1L)));
        when(users.findById(anyLong())).thenReturn(Optional.empty());
        List<VehicleListItemResponse> result = service.listAccessibleVehicles(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).ownerEmail()).isEqualTo("unknown");
        assertThat(result.get(0).canEdit()).isTrue();
        assertThat(result.get(0).canDelete()).isTrue();
        assertThat(result.get(0).activeByMe()).isTrue();
        assertThat(result.get(0).activeByEmail()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Pasiekiamų mašinų sąrašas, kai vartotojas turi grupiu")
    void listAccessibleVehiclesIncludesGroupVehiclesWhenUserHasGroups() {
        Vehicle ownedVehicle = vehicle(1L, 10L, null);
        Vehicle ownedVehicleInGroup = vehicle(2L, 10L, 5L);
        Vehicle groupVehicle = vehicle(3L, 20L, 5L);

        var membership = mock(com.example.back.group.domain.GroupMember.class);
        when(membership.getGroupId()).thenReturn(5L);

        var ownerAccount = mock(com.example.back.auth.domain.Account.class);
        when(ownerAccount.getEmail()).thenReturn("owner@test.com");

        var otherAccount = mock(com.example.back.auth.domain.Account.class);
        when(otherAccount.getEmail()).thenReturn("other@test.com");

        when(vehicles.findAllByOwnerUserId(10L)).thenReturn(List.of(ownedVehicle, ownedVehicleInGroup));
        when(groupMembers.findAllByUserId(10L)).thenReturn(List.of(membership));
        when(vehicles.findAllByGroupIdIn(Collections.singleton(5L))).thenReturn(List.of(ownedVehicleInGroup, groupVehicle));
        when(activeVehicles.findByUserId(10L)).thenReturn(Optional.of(new ActiveVehicle(10L, 3L)));
        when(activeVehicles.findAll()).thenReturn(List.of(new ActiveVehicle(10L, 3L), new ActiveVehicle(99L, 2L)));
        when(users.findById(10L)).thenReturn(Optional.of(ownerAccount));
        when(users.findById(20L)).thenReturn(Optional.of(otherAccount));
        when(users.findById(99L)).thenReturn(Optional.empty());

        List<VehicleListItemResponse> result = service.listAccessibleVehicles(10L);

        assertThat(result).hasSize(3);

        VehicleListItemResponse first = result.get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.canEdit()).isTrue();
        assertThat(first.canDelete()).isTrue();
        assertThat(first.activeByMe()).isFalse();
        assertThat(first.activeByEmail()).isNull();
        assertThat(first.ownerEmail()).isEqualTo("owner@test.com");

        VehicleListItemResponse second = result.get(1);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(second.canEdit()).isTrue();
        assertThat(second.canDelete()).isFalse();
        assertThat(second.activeByMe()).isFalse();
        assertThat(second.activeByEmail()).isEqualTo("unknown");
        assertThat(second.ownerEmail()).isEqualTo("owner@test.com");

        VehicleListItemResponse third = result.get(2);
        assertThat(third.id()).isEqualTo(3L);
        assertThat(third.canEdit()).isFalse();
        assertThat(third.canDelete()).isFalse();
        assertThat(third.activeByMe()).isTrue();
        assertThat(third.activeByEmail()).isEqualTo("owner@test.com");
        assertThat(third.ownerEmail()).isEqualTo("other@test.com");
    }

    @Test
    @DisplayName("Pasiekiamų mašinų sąrašas, kai nėra aktyvios mašinos")
    void listAccessibleVehiclesWhenUserHasNoActiveVehicle() {
        Vehicle vehicle = vehicle(1L, 10L, null);

        var account = mock(com.example.back.auth.domain.Account.class);
        when(account.getEmail()).thenReturn("owner@test.com");

        when(vehicles.findAllByOwnerUserId(10L)).thenReturn(List.of(vehicle));
        when(groupMembers.findAllByUserId(10L)).thenReturn(List.of());
        when(activeVehicles.findByUserId(10L)).thenReturn(Optional.empty());
        when(activeVehicles.findAll()).thenReturn(List.of());
        when(users.findById(10L)).thenReturn(Optional.of(account));

        List<VehicleListItemResponse> result = service.listAccessibleVehicles(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).activeByMe()).isFalse();
        assertThat(result.get(0).activeByEmail()).isNull();
        assertThat(result.get(0).ownerEmail()).isEqualTo("owner@test.com");
    }

    @Test
    @DisplayName("Aktyvi mašina nustatoma, kai vartotojas yra savininkas")
    void setActiveVehicleWorksForOwner() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.empty());

        service.setActiveVehicle(10L, 1L);

        verify(activeVehicles).deleteByUserId(10L);

        ArgumentCaptor<ActiveVehicle> captor = ArgumentCaptor.forClass(ActiveVehicle.class);
        verify(activeVehicles).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getVehicleId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Aktyvi mašina nustatoma, kai vartotojas yra grupes narys")
    void setActiveVehicleWorksForGroupMember() {
        Vehicle vehicle = vehicle(1L, 20L, 5L);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(groupMembers.existsByGroupIdAndUserId(5L, 10L)).thenReturn(true);
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.empty());

        service.setActiveVehicle(10L, 1L);

        verify(activeVehicles).save(any(ActiveVehicle.class));
    }

    @Test
    @DisplayName("Aktyvi mašina meta klaidą, kai mašina nerasta")
    void setActiveVehicleThrowsWhenMissing() {
        when(vehicles.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActiveVehicle(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Aktyvi mašina meta klaidą, kai nėra prieigos")
    void setActiveVehicleThrowsWhenNoAccess() {
        Vehicle vehicle = vehicle(1L, 20L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.setActiveVehicle(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios mašinos");
    }

    @Test
    @DisplayName("Aktyvi mašina neleidziama kai vartotojas nėra grupės narys")
    void setActiveVehicleThrowsWhenUserIsNotGroupMember() {
        Vehicle vehicle = vehicle(1L, 20L, 5L);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(groupMembers.existsByGroupIdAndUserId(5L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.setActiveVehicle(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios mašinos");

        verify(activeVehicles, never()).save(any());
    }

    @Test
    @DisplayName("Aktyvi mašina leidžiama kai vartotojas yra grupės narys")
    void setActiveVehicleAllowsGroupMember() {
        Vehicle vehicle = vehicle(1L, 20L, 5L);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(groupMembers.existsByGroupIdAndUserId(5L, 10L)).thenReturn(true);
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.empty());

        service.setActiveVehicle(10L, 1L);

        verify(activeVehicles).deleteByUserId(10L);
        verify(activeVehicles).save(any(ActiveVehicle.class));
    }

    @Test
    @DisplayName("Aktyvi mašina meta klaidą kai ją naudoja kitas vartotojas")
    void setActiveVehicleThrowsWhenUsedByOtherUser() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.of(new ActiveVehicle(99L, 1L)));

        assertThatThrownBy(() -> service.setActiveVehicle(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Šią mašiną jau naudoja kitas vartotojas");
    }

    @Test
    @DisplayName("Aktyvi masina leidžiama kai ją jau naudoja tas pats vartotojas")
    void setActiveVehicleAllowsVehicleAlreadyUsedBySameUser() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(activeVehicles.findByVehicleId(1L)).thenReturn(Optional.of(new ActiveVehicle(10L, 1L)));

        service.setActiveVehicle(10L, 1L);

        verify(activeVehicles).deleteByUserId(10L);
        verify(activeVehicles).save(any(ActiveVehicle.class));
    }

    @Test
    @DisplayName("Aktyvios mašinos išvalymas ištrina pagal vartotoja")
    void clearActiveVehicleDeletesByUserId() {
        service.clearActiveVehicle(10L);

        verify(activeVehicles).deleteByUserId(10L);
    }

    @Test
    @DisplayName("Detalės grąžinamos savininkui")
    void getVehicleDetailsForOwner() {
        Vehicle vehicle = vehicle(1L, 10L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));

        VehicleDetailsResponse result = service.getVehicleDetailsForViewer(10L, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.canManageReminders()).isTrue();
    }

    @Test
    @DisplayName("Detalės grąžinamos grupės nariui")
    void getVehicleDetailsForGroupMember() {
        Vehicle vehicle = vehicle(1L, 20L, 5L);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(groupMembers.existsByGroupIdAndUserId(5L, 10L)).thenReturn(true);

        VehicleDetailsResponse result = service.getVehicleDetailsForViewer(10L, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.canManageReminders()).isFalse();
    }

    @Test
    @DisplayName("Detalės grąžinamos grupės nariui ,kuris nėra savininkas")
    void getVehicleDetailsReturnsForGroupMemberWhoIsNotOwner() {
        Vehicle vehicle = vehicle(1L, 20L, 5L);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(groupMembers.existsByGroupIdAndUserId(5L, 10L)).thenReturn(true);

        VehicleDetailsResponse result = service.getVehicleDetailsForViewer(10L, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.groupId()).isEqualTo(5L);
        assertThat(result.canManageReminders()).isFalse();
    }

    @Test
    @DisplayName("Detalės meta klaidą, kai mašina nerasta")
    void getVehicleDetailsThrowsWhenMissing() {
        when(vehicles.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVehicleDetailsForViewer(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Mašina nerasta");
    }

    @Test
    @DisplayName("Detalės meta klaidą ,kai nėra prieigos")
    void getVehicleDetailsThrowsWhenNoAccess() {
        Vehicle vehicle = vehicle(1L, 20L, null);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.getVehicleDetailsForViewer(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios mašinos");
    }

    @Test
    @DisplayName("Detalės negrąžinamos, kai vartotojas nėra grupės narys")
    void getVehicleDetailsThrowsWhenUserIsNotGroupMember() {
        Vehicle vehicle = vehicle(1L, 20L, 5L);
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle));
        when(groupMembers.existsByGroupIdAndUserId(5L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.getVehicleDetailsForViewer(10L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios mašinos");
    }

    private static Vehicle vehicle(Long id, Long ownerUserId, Long groupId) {
        Vehicle vehicle = new Vehicle(ownerUserId);
        vehicle.setId(id);
        vehicle.setGroupId(groupId);
        vehicle.setVin("WVWZZZ1JZXW000001");
        vehicle.setMake("Volkswagen");
        vehicle.setModel("Golf");
        vehicle.setModelYear(2000);
        vehicle.setEngineDisplacementCc(1900);
        vehicle.setFuelType("DIESEL");
        vehicle.setOdometerKm(1000L);
        vehicle.setTransmission("Manual");
        vehicle.setDrive("FWD");
        vehicle.setBody("Hatchback");
        vehicle.setDoors(5);
        vehicle.setSeats(5);
        vehicle.setCo2Gkm(new BigDecimal("140.50"));
        vehicle.setPlantCountry("Germany");
        vehicle.setManufacturer("Volkswagen AG");
        return vehicle;
    }

    private static VehicleConfirmRequest confirmRequest(String vin) {
        return new VehicleConfirmRequest(vin, "Volkswagen", "Golf", 2000, 1900, "Manual", 1000L, "DIESEL", "FWD", "Hatchback", 5, 5, new BigDecimal("140.50"), "Germany", "Volkswagen AG");
    }

    private static VehicleUpdateRequest updateRequest() {
        return new VehicleUpdateRequest("Volkswagen", "Golf", 2001, 2000, "DIESEL", 2000L, "Manual", "FWD", "Hatchback", 5, 5, 150.5, "Germany", "VW AG");
    }

    private static VincarioDecodeResponse decodedVehicle() {
        VincarioDecodeResponse response = new VincarioDecodeResponse();
        response.decode = List.of(
                item("Make", "Volkswagen"),
                item("Model", "Golf"),
                item("Model Year", 2000),
                item("Engine Displacement (ccm)", 1900),
                item("Transmission", "Manual"),
                item("Fuel Type - Primary", "Diesel"),
                item("Drive", "FWD"),
                item("Body", "Hatchback"),
                item("Number of Doors", 5),
                item("Number of Seats", 5),
                item("Average CO2 Emission (g/km)", 140.0),
                item("Plant Country", "Germany"),
                item("Manufacturer", "Volkswagen AG")
        );
        return response;
    }

    private static VincarioDecodeResponse.DecodeItem item(String label, Object value) {
        VincarioDecodeResponse.DecodeItem item = new VincarioDecodeResponse.DecodeItem();
        item.label = label;
        item.value = value;
        return item;
    }
}