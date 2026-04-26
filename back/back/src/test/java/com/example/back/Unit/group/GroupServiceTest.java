package com.example.back.Unit.group;

import com.example.back.auth.domain.Account;
import com.example.back.auth.repo.AccountRepo;
import com.example.back.group.domain.AppGroup;
import com.example.back.group.domain.GroupMember;
import com.example.back.group.domain.GroupMemberId;
import com.example.back.group.repo.AppGroupRepo;
import com.example.back.group.repo.GroupMemberRepo;
import com.example.back.group.service.GroupService;
import com.example.back.vehicle.domain.ActiveVehicle;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.repo.ActiveVehicleRepo;
import com.example.back.vehicle.repo.VehicleRepo;
import com.example.back.vehicle.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    private final AppGroupRepo groups = mock(AppGroupRepo.class);
    private final GroupMemberRepo members = mock(GroupMemberRepo.class);
    private final AccountRepo users = mock(AccountRepo.class);
    private final VehicleService vehicleService = mock(VehicleService.class);
    private final VehicleRepo vehicleRepository = mock(VehicleRepo.class);
    private final ActiveVehicleRepo activeVehicles = mock(ActiveVehicleRepo.class);
    private final GroupService service = new GroupService(groups, members, users, vehicleService, vehicleRepository, activeVehicles);

    @Test
    @DisplayName("Sukuria grupe ir owner nari")
    void createGroupCreatesGroupAndOwnerMember() {
        AppGroup saved = group(1L, "Family", 10L);
        when(groups.save(any(AppGroup.class))).thenReturn(saved);

        AppGroup result = service.createGroup(10L, "Family");

        assertThat(result).isSameAs(saved);
        verify(groups).save(any(AppGroup.class));
        verify(members).save(argThat(m -> m.getGroupId().equals(1L) && m.getUserId().equals(10L) && m.getRole().equals("OWNER")));
    }

    @Test
    @DisplayName("Grazina mano narystes")
    void myMembershipsReturnsMemberships() {
        List<GroupMember> resultList = List.of(new GroupMember(1L, 10L, "OWNER"));
        when(members.findAllByUserId(10L)).thenReturn(resultList);

        List<GroupMember> result = service.myMemberships(10L);

        assertThat(result).isEqualTo(resultList);
    }

    @Test
    @DisplayName("Grazina grupe kai ji rasta")
    void getGroupOrThrowReturnsGroup() {
        AppGroup group = group(1L, "Family", 10L);
        when(groups.findById(1L)).thenReturn(Optional.of(group));

        AppGroup result = service.getGroupOrThrow(1L);

        assertThat(result).isSameAs(group);
    }

    @Test
    @DisplayName("Meta klaida kai grupe nerasta")
    void getGroupOrThrowThrowsWhenMissing() {
        when(groups.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGroupOrThrow(1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Grupė nerasta");
    }

    @Test
    @DisplayName("RequireMember grazina nari")
    void requireMemberReturnsMember() {
        GroupMember member = new GroupMember(1L, 10L, "MEMBER");
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(member));

        GroupMember result = service.requireMember(1L, 10L);

        assertThat(result).isSameAs(member);
    }

    @Test
    @DisplayName("RequireMember meta klaida kai nario nera")
    void requireMemberThrowsWhenMissing() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireMember(1L, 10L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos prie šios grupės");
    }

    @Test
    @DisplayName("RequireOwner leidzia owner")
    void requireOwnerAllowsOwner() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));

        service.requireOwner(1L, 10L);

        verify(members).findByGroupIdAndUserId(1L, 10L);
    }

    @Test
    @DisplayName("RequireOwner meta klaida kai ne owner")
    void requireOwnerThrowsWhenNotOwner() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "MEMBER")));

        assertThatThrownBy(() -> service.requireOwner(1L, 10L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Tik savininkas gali atlikti šį veiksmą");
    }

    @Test
    @DisplayName("Pervadina grupe kai vartotojas owner")
    void renameGroupRenamesGroup() {
        AppGroup group = group(1L, "Old", 10L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(groups.findById(1L)).thenReturn(Optional.of(group));

        AppGroup result = service.renameGroup(1L, 10L, "New");

        assertThat(result.getName()).isEqualTo("New");
    }

    @Test
    @DisplayName("Invite meta klaida kai vartotojas neegzistuoja")
    void inviteThrowsWhenUserDoesNotExist() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(users.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inviteAndAutoAdd(1L, 10L, "USER@Test.COM")).isInstanceOf(IllegalArgumentException.class).hasMessage("Toks vartotojas neegzistuoja");
    }

    @Test
    @DisplayName("Invite grazina zinute kai vartotojas jau grupeje")
    void inviteReturnsMessageWhenUserAlreadyInGroup() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(20L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(account));
        when(members.existsByGroupIdAndUserId(1L, 20L)).thenReturn(true);

        String result = service.inviteAndAutoAdd(1L, 10L, "USER@Test.COM");

        assertThat(result).isEqualTo("Vartotojas jau yra grupėje");
        verify(members, never()).save(argThat(m -> m.getUserId().equals(20L)));
    }

    @Test
    @DisplayName("Invite prideda vartotoja i grupe")
    void inviteAddsUserToGroup() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(20L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(users.findByEmail("user@test.com")).thenReturn(Optional.of(account));
        when(members.existsByGroupIdAndUserId(1L, 20L)).thenReturn(false);

        String result = service.inviteAndAutoAdd(1L, 10L, "USER@Test.COM");

        assertThat(result).isEqualTo("Pakvietimas išsiųstas (demo) ir vartotojas automatiškai pridėtas į grupę.");
        verify(members).save(argThat(m -> m.getGroupId().equals(1L) && m.getUserId().equals(20L) && m.getRole().equals("MEMBER")));
    }

    @Test
    @DisplayName("RemoveMember meta klaida kai bandoma pasalinti owner")
    void removeMemberThrowsWhenRemovingOwner() {
        AppGroup group = group(1L, "Family", 10L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(groups.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.removeMember(1L, 10L, 10L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Negalima pašalinti savininko");
    }

    @Test
    @DisplayName("RemoveMember istrina aktyvia masina jei ji yra toje grupeje")
    void removeMemberDeletesActiveVehicleWhenItBelongsToGroup() {
        AppGroup group = group(1L, "Family", 10L);
        Vehicle vehicle = vehicle(5L, 10L, 1L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(groups.findById(1L)).thenReturn(Optional.of(group));
        when(activeVehicles.findByUserId(20L)).thenReturn(Optional.of(new ActiveVehicle(20L, 5L)));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        service.removeMember(1L, 10L, 20L);

        verify(activeVehicles).deleteByUserId(20L);
        verify(members).deleteById(new GroupMemberId(1L, 20L));
    }

    @Test
    @DisplayName("RemoveMember neistrina aktyvios masinos jei ji ne toje grupeje")
    void removeMemberDoesNotDeleteActiveVehicleWhenItBelongsToOtherGroup() {
        AppGroup group = group(1L, "Family", 10L);
        Vehicle vehicle = vehicle(5L, 10L, 2L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(groups.findById(1L)).thenReturn(Optional.of(group));
        when(activeVehicles.findByUserId(20L)).thenReturn(Optional.of(new ActiveVehicle(20L, 5L)));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        service.removeMember(1L, 10L, 20L);

        verify(activeVehicles, never()).deleteByUserId(20L);
        verify(members).deleteById(new GroupMemberId(1L, 20L));
    }

    @Test
    @DisplayName("LeaveGroup meta klaida kai owner bando iseiti pagal role")
    void leaveGroupThrowsWhenOwnerRoleLeaves() {
        AppGroup group = group(1L, "Family", 10L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(groups.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.leaveGroup(1L, 10L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Savininkas negali išeiti");
    }

    @Test
    @DisplayName("LeaveGroup meta klaida kai owner bando iseiti pagal group owner")
    void leaveGroupThrowsWhenGroupOwnerLeavesEvenIfRoleMember() {
        AppGroup group = group(1L, "Family", 10L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "MEMBER")));
        when(groups.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.leaveGroup(1L, 10L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Savininkas negali išeiti");
    }

    @Test
    @DisplayName("LeaveGroup istrina nari ir aktyvia masina")
    void leaveGroupDeletesMemberAndActiveVehicle() {
        AppGroup group = group(1L, "Family", 10L);
        GroupMember member = new GroupMember(1L, 20L, "MEMBER");
        Vehicle vehicle = vehicle(5L, 10L, 1L);
        when(members.findByGroupIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));
        when(groups.findById(1L)).thenReturn(Optional.of(group));
        when(activeVehicles.findByUserId(20L)).thenReturn(Optional.of(new ActiveVehicle(20L, 5L)));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        service.leaveGroup(1L, 20L);

        verify(activeVehicles).deleteByUserId(20L);
        verify(members).delete(member);
    }

    @Test
    @DisplayName("LeaveGroup istrina tik nari kai aktyvios masinos nera")
    void leaveGroupDeletesOnlyMemberWhenNoActiveVehicle() {
        AppGroup group = group(1L, "Family", 10L);
        GroupMember member = new GroupMember(1L, 20L, "MEMBER");
        when(members.findByGroupIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));
        when(groups.findById(1L)).thenReturn(Optional.of(group));
        when(activeVehicles.findByUserId(20L)).thenReturn(Optional.empty());

        service.leaveGroup(1L, 20L);

        verify(activeVehicles, never()).deleteByUserId(20L);
        verify(members).delete(member);
    }

    @Test
    @DisplayName("AddVehicleToGroup deleguoja i VehicleService")
    void addVehicleToGroupDelegatesToVehicleService() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));

        service.addVehicleToGroup(1L, 10L, 5L);

        verify(vehicleService).assignToGroup(10L, 5L, 1L);
    }

    @Test
    @DisplayName("RemoveVehicleFromGroup istrina aktyvia masina ir deleguoja")
    void removeVehicleFromGroupDeletesActiveVehicleAndDelegates() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));

        service.removeVehicleFromGroup(1L, 10L, 5L);

        verify(activeVehicles).deleteByVehicleId(5L);
        verify(vehicleService).removeFromGroup(10L, 5L, 1L);
    }

    @Test
    @DisplayName("ListMembers grazina narius")
    void listMembersReturnsMembers() {
        List<GroupMember> groupMembers = List.of(new GroupMember(1L, 10L, "OWNER"));
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(members.findAllByGroupId(1L)).thenReturn(groupMembers);

        List<GroupMember> result = service.listMembers(1L, 10L);

        assertThat(result).isEqualTo(groupMembers);
    }

    @Test
    @DisplayName("DeleteGroup istrina aktyvias masinas kai yra masinu")
    void deleteGroupDeletesActiveVehiclesWhenVehiclesExist() {
        Vehicle first = vehicle(5L, 10L, 1L);
        Vehicle second = vehicle(6L, 10L, 1L);
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(vehicleRepository.findAllByGroupId(1L)).thenReturn(List.of(first, second));

        service.deleteGroup(1L, 10L);

        verify(activeVehicles).deleteByVehicleIdIn(List.of(5L, 6L));
        verify(groups).deleteById(1L);
    }

    @Test
    @DisplayName("DeleteGroup istrina grupe kai nera masinu")
    void deleteGroupDeletesGroupWhenNoVehiclesExist() {
        when(members.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(new GroupMember(1L, 10L, "OWNER")));
        when(vehicleRepository.findAllByGroupId(1L)).thenReturn(List.of());

        service.deleteGroup(1L, 10L);

        verify(activeVehicles, never()).deleteByVehicleIdIn(anyCollection());
        verify(groups).deleteById(1L);
    }

    private static AppGroup group(Long id, String name, Long ownerUserId) {
        AppGroup group = new AppGroup(name, ownerUserId);
        setId(group, id);
        return group;
    }

    private static Vehicle vehicle(Long id, Long ownerUserId, Long groupId) {
        Vehicle vehicle = new Vehicle(ownerUserId);
        vehicle.setId(id);
        vehicle.setGroupId(groupId);
        return vehicle;
    }

    private static void setId(AppGroup group, Long id) {
        try {
            Field field = AppGroup.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(group, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}