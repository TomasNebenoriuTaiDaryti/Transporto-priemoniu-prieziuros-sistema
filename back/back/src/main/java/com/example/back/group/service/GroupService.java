package com.example.back.group.service;

import com.example.back.auth.repo.AccountRepo;
import com.example.back.group.domain.AppGroup;
import com.example.back.group.domain.GroupMember;
import com.example.back.group.domain.GroupMemberId;
import com.example.back.group.repo.AppGroupRepo;
import com.example.back.group.repo.GroupMemberRepo;
import com.example.back.vehicle.repo.ActiveVehicleRepo;
import com.example.back.vehicle.repo.VehicleRepo;
import com.example.back.vehicle.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final AppGroupRepo groups;
    private final GroupMemberRepo members;
    private final AccountRepo users;
    private final VehicleService vehicleService;
    private final VehicleRepo vehicleRepository;
    private final ActiveVehicleRepo activeVehicles;

    public GroupService(
            AppGroupRepo groups,
            GroupMemberRepo members,
            AccountRepo users,
            VehicleService vehicleService,
            VehicleRepo vehicleRepository,
            ActiveVehicleRepo activeVehicles
    ) {
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.vehicleService = vehicleService;
        this.vehicleRepository = vehicleRepository;
        this.activeVehicles = activeVehicles;
    }

    @Transactional
    public AppGroup createGroup(Long creatorUserId, String name) {
        AppGroup g = groups.save(new AppGroup(name, creatorUserId));
        members.save(new GroupMember(g.getId(), creatorUserId, "OWNER"));
        return g;
    }

    public List<GroupMember> myMemberships(Long userId) {
        return members.findAllByUserId(userId);
    }

    public AppGroup getGroupOrThrow(Long groupId) {
        return groups.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Grupė nerasta"));
    }

    public GroupMember requireMember(Long groupId, Long userId) {
        return members.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Neturi prieigos prie šios grupės"));
    }

    public void requireOwner(Long groupId, Long userId) {
        GroupMember m = requireMember(groupId, userId);
        if (!"OWNER".equals(m.getRole())) {
            throw new IllegalArgumentException("Tik savininkas gali atlikti šį veiksmą");
        }
    }

    @Transactional
    public AppGroup renameGroup(Long groupId, Long userId, String name) {
        requireOwner(groupId, userId);
        AppGroup g = getGroupOrThrow(groupId);
        g.setName(name);
        return g;
    }

    @Transactional
    public String inviteAndAutoAdd(Long groupId, Long ownerUserId, String email) {
        requireOwner(groupId, ownerUserId);

        var uOpt = users.findByEmail(email.toLowerCase());
        if (uOpt.isEmpty()) {
            throw new IllegalArgumentException("Toks vartotojas neegzistuoja");
        }
        Long invitedUserId = uOpt.get().getId();

        if (members.existsByGroupIdAndUserId(groupId, invitedUserId)) {
            return "Vartotojas jau yra grupėje";
        }

        members.save(new GroupMember(groupId, invitedUserId, "MEMBER"));
        return "Pakvietimas išsiųstas (demo) ir vartotojas automatiškai pridėtas į grupę.";
    }

    @Transactional
    public void removeMember(Long groupId, Long ownerUserId, Long memberUserId) {
        requireOwner(groupId, ownerUserId);

        AppGroup g = getGroupOrThrow(groupId);
        if (g.getOwnerUserId().equals(memberUserId)) {
            throw new IllegalArgumentException("Negalima pašalinti savininko");
        }

        activeVehicles.findByUserId(memberUserId).ifPresent(av -> {
            vehicleRepository.findById(av.getVehicleId()).ifPresent(v -> {
                if (v.getGroupId() != null && v.getGroupId().equals(groupId)) {
                    activeVehicles.deleteByUserId(memberUserId);
                }
            });
        });

        members.deleteById(new GroupMemberId(groupId, memberUserId));
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember m = requireMember(groupId, userId);
        AppGroup g = getGroupOrThrow(groupId);

        if ("OWNER".equals(m.getRole()) || g.getOwnerUserId().equals(userId)) {
            throw new IllegalArgumentException("Savininkas negali išeiti");
        }

        activeVehicles.findByUserId(userId).ifPresent(av -> {
            vehicleRepository.findById(av.getVehicleId()).ifPresent(v -> {
                if (v.getGroupId() != null && v.getGroupId().equals(groupId)) {
                    activeVehicles.deleteByUserId(userId);
                }
            });
        });

        members.delete(m);
    }

    @Transactional
    public void addVehicleToGroup(Long groupId, Long ownerUserId, Long vehicleId) {
        requireOwner(groupId, ownerUserId);
        vehicleService.assignToGroup(ownerUserId, vehicleId, groupId);
    }

    @Transactional
    public void removeVehicleFromGroup(Long groupId, Long ownerUserId, Long vehicleId) {
        requireOwner(groupId, ownerUserId);
        activeVehicles.deleteByVehicleId(vehicleId);
        vehicleService.removeFromGroup(ownerUserId, vehicleId, groupId);
    }

    public List<GroupMember> listMembers(Long groupId, Long userId) {
        requireMember(groupId, userId);
        return members.findAllByGroupId(groupId);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long ownerUserId) {
        requireOwner(groupId, ownerUserId);
        var groupVehicles = vehicleRepository.findAllByGroupId(groupId);
        var ids = groupVehicles.stream().map(v -> v.getId()).toList();
        if (!ids.isEmpty()) {
            activeVehicles.deleteByVehicleIdIn(ids);
        }

        groups.deleteById(groupId);
    }
}
