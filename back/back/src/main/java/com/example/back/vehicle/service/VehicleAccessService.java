package com.example.back.vehicle.service;


import com.example.back.group.repo.GroupMemberRepo;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.repo.VehicleRepo;
import org.springframework.stereotype.Service;

@Service
public class VehicleAccessService {

    private final VehicleRepo vehicles;
    private final GroupMemberRepo groupMembers;

    public VehicleAccessService(VehicleRepo vehicles, GroupMemberRepo groupMembers) {
        this.vehicles = vehicles;
        this.groupMembers = groupMembers;
    }

    public Vehicle getVehicleOrThrow(Long vehicleId) {
        return vehicles.findById(vehicleId).orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));
    }

    public boolean canView(Long userId, Vehicle v) {
        if (v.getOwnerUserId().equals(userId)) return true;
        if (v.getGroupId() == null) return false;
        return groupMembers.existsByGroupIdAndUserId(v.getGroupId(), userId);
    }

    public boolean isVehicleOwner(Long userId, Vehicle v) {
        return v.getOwnerUserId().equals(userId);
    }
}
