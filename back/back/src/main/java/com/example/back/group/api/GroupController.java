package com.example.back.group.api;

import com.example.back.auth.repo.AccountRepo;
import com.example.back.group.dto.GroupDtos;
import com.example.back.group.service.GroupService;
import com.example.back.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final AccountRepo users;

    public GroupController(GroupService groupService, AccountRepo users) {
        this.groupService = groupService;
        this.users = users;
    }

    @PostMapping
    public GroupDtos.GroupResponse create(@Valid @RequestBody GroupDtos.CreateGroupRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        var g = groupService.createGroup(userId, req.name());
        return new GroupDtos.GroupResponse(g.getId(), g.getName(), "OWNER", g.getOwnerUserId());
    }

    @GetMapping
    public List<GroupDtos.GroupResponse> myGroups(Authentication auth) {
        Long userId = AuthUser.userId(auth);

        return groupService.myMemberships(userId).stream().map(m -> {
            var g = groupService.getGroupOrThrow(m.getGroupId());
            return new GroupDtos.GroupResponse(g.getId(), g.getName(), m.getRole(), g.getOwnerUserId());
        }).toList();
    }

    @PatchMapping("/{groupId}")
    public GroupDtos.GroupResponse rename(@PathVariable Long groupId,
                                          @Valid @RequestBody GroupDtos.UpdateGroupRequest req,
                                          Authentication auth) {
        Long userId = AuthUser.userId(auth);
        var g = groupService.renameGroup(groupId, userId, req.name());
        var my = groupService.requireMember(groupId, userId);
        return new GroupDtos.GroupResponse(g.getId(), g.getName(), my.getRole(), g.getOwnerUserId());
    }

    @PostMapping("/{groupId}/invite")
    public GroupDtos.SimpleMessage invite(@PathVariable Long groupId,
                                          @Valid @RequestBody GroupDtos.InviteRequest req,
                                          Authentication auth) {
        Long userId = AuthUser.userId(auth);
        String msg = groupService.inviteAndAutoAdd(groupId, userId, req.email());
        return new GroupDtos.SimpleMessage(msg);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupDtos.MemberResponse> members(@PathVariable Long groupId, Authentication auth) {
        Long userId = AuthUser.userId(auth);

        return groupService.listMembers(groupId, userId).stream().map(m -> {
            var u = users.findById(m.getUserId()).orElseThrow();
            return new GroupDtos.MemberResponse(u.getId(), u.getEmail(), m.getRole());
        }).toList();
    }

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    public void removeMember(@PathVariable Long groupId, @PathVariable Long memberUserId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        groupService.removeMember(groupId, userId, memberUserId);
    }

    @PostMapping("/{groupId}/vehicles/{vehicleId}")
    public void addVehicle(@PathVariable Long groupId, @PathVariable Long vehicleId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        groupService.addVehicleToGroup(groupId, userId, vehicleId);
    }

    @DeleteMapping("/{groupId}/vehicles/{vehicleId}")
    public void removeVehicle(@PathVariable Long groupId, @PathVariable Long vehicleId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        groupService.removeVehicleFromGroup(groupId, userId, vehicleId);
    }

    @PostMapping("/{groupId}/leave")
    public void leave(@PathVariable Long groupId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        groupService.leaveGroup(groupId, userId);
    }

    @DeleteMapping("/{groupId}")
    public void deleteGroup(@PathVariable Long groupId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        groupService.deleteGroup(groupId, userId);
    }
}
