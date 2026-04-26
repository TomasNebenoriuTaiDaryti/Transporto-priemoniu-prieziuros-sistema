package com.example.back.Unit.group;

import com.example.back.auth.domain.Account;
import com.example.back.auth.repo.AccountRepo;
import com.example.back.group.api.GroupController;
import com.example.back.group.domain.AppGroup;
import com.example.back.group.domain.GroupMember;
import com.example.back.group.dto.GroupDtos;
import com.example.back.group.service.GroupService;
import com.example.back.security.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupControllerTest {

    private final GroupService groupService = mock(GroupService.class);
    private final AccountRepo users = mock(AccountRepo.class);
    private final GroupController controller = new GroupController(groupService, users);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Sukuria grupe ir grazina owner role")
    void createCreatesGroupAndReturnsOwnerRole() {
        AppGroup group = group(1L, "Family", 10L);
        GroupDtos.CreateGroupRequest request = new GroupDtos.CreateGroupRequest("Family");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(groupService.createGroup(10L, "Family")).thenReturn(group);

            GroupDtos.GroupResponse result = controller.create(request, auth);

            assertThat(result).isEqualTo(new GroupDtos.GroupResponse(1L, "Family", "OWNER", 10L));
            verify(groupService).createGroup(10L, "Family");
        }
    }

    @Test
    @DisplayName("Grazina mano grupes")
    void myGroupsReturnsMyGroups() {
        GroupMember member = new GroupMember(1L, 10L, "MEMBER");
        AppGroup group = group(1L, "Family", 20L);

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(groupService.myMemberships(10L)).thenReturn(List.of(member));
            when(groupService.getGroupOrThrow(1L)).thenReturn(group);

            List<GroupDtos.GroupResponse> result = controller.myGroups(auth);

            assertThat(result).containsExactly(new GroupDtos.GroupResponse(1L, "Family", "MEMBER", 20L));
            verify(groupService).myMemberships(10L);
            verify(groupService).getGroupOrThrow(1L);
        }
    }

    @Test
    @DisplayName("Pervadina grupe ir grazina mano role")
    void renameRenamesGroupAndReturnsMyRole() {
        AppGroup group = group(1L, "New family", 10L);
        GroupMember member = new GroupMember(1L, 10L, "OWNER");
        GroupDtos.UpdateGroupRequest request = new GroupDtos.UpdateGroupRequest("New family");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(groupService.renameGroup(1L, 10L, "New family")).thenReturn(group);
            when(groupService.requireMember(1L, 10L)).thenReturn(member);

            GroupDtos.GroupResponse result = controller.rename(1L, request, auth);

            assertThat(result).isEqualTo(new GroupDtos.GroupResponse(1L, "New family", "OWNER", 10L));
            verify(groupService).renameGroup(1L, 10L, "New family");
            verify(groupService).requireMember(1L, 10L);
        }
    }

    @Test
    @DisplayName("Pakvietimas grazina paprasta zinute")
    void inviteReturnsSimpleMessage() {
        GroupDtos.InviteRequest request = new GroupDtos.InviteRequest("user@test.com");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(groupService.inviteAndAutoAdd(1L, 10L, "user@test.com")).thenReturn("ok");

            GroupDtos.SimpleMessage result = controller.invite(1L, request, auth);

            assertThat(result.message()).isEqualTo("ok");
            verify(groupService).inviteAndAutoAdd(1L, 10L, "user@test.com");
        }
    }

    @Test
    @DisplayName("Grazina grupes narius")
    void membersReturnsGroupMembers() {
        GroupMember member = new GroupMember(1L, 20L, "MEMBER");
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(20L);
        when(account.getEmail()).thenReturn("user@test.com");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(groupService.listMembers(1L, 10L)).thenReturn(List.of(member));
            when(users.findById(20L)).thenReturn(Optional.of(account));

            List<GroupDtos.MemberResponse> result = controller.members(1L, auth);

            assertThat(result).containsExactly(new GroupDtos.MemberResponse(20L, "user@test.com", "MEMBER"));
            verify(groupService).listMembers(1L, 10L);
            verify(users).findById(20L);
        }
    }

    @Test
    @DisplayName("Pasalina nari is grupes")
    void removeMemberDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.removeMember(1L, 20L, auth);

            verify(groupService).removeMember(1L, 10L, 20L);
        }
    }

    @Test
    @DisplayName("Prideda masina i grupe")
    void addVehicleDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.addVehicle(1L, 5L, auth);

            verify(groupService).addVehicleToGroup(1L, 10L, 5L);
        }
    }

    @Test
    @DisplayName("Pasalina masina is grupes")
    void removeVehicleDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.removeVehicle(1L, 5L, auth);

            verify(groupService).removeVehicleFromGroup(1L, 10L, 5L);
        }
    }

    @Test
    @DisplayName("Palieka grupe")
    void leaveDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.leave(1L, auth);

            verify(groupService).leaveGroup(1L, 10L);
        }
    }

    @Test
    @DisplayName("Istrina grupe")
    void deleteGroupDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.deleteGroup(1L, auth);

            verify(groupService).deleteGroup(1L, 10L);
        }
    }

    private static AppGroup group(Long id, String name, Long ownerUserId) {
        AppGroup group = new AppGroup(name, ownerUserId);
        setId(group, id);
        return group;
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