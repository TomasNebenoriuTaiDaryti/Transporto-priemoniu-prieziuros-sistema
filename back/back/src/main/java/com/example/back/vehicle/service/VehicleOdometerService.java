package com.example.back.vehicle.service;


import com.example.back.vehicle.repo.ActiveVehicleRepo;
import com.example.back.vehicle.repo.VehicleRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleOdometerService {

    private final ActiveVehicleRepo activeVehicles;
    private final VehicleRepo vehicles;

    public VehicleOdometerService(ActiveVehicleRepo activeVehicles, VehicleRepo vehicles) {
        this.activeVehicles = activeVehicles;
        this.vehicles = vehicles;
    }

    @Transactional
    public void appendKmIfActiveUser(Long userId, Long vehicleId, Long deltaKm) {
        if (deltaKm == null || deltaKm <= 0) return;

        var av = activeVehicles.findByVehicleId(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Ši mašina šiuo metu nenaudojama"));

        if (!av.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Negali keisti ridos: šią mašiną naudoja kitas vartotojas");
        }

        int updated = vehicles.addOdometerKm(vehicleId, deltaKm);
        if (updated == 0) throw new IllegalArgumentException("Mašina nerasta");
    }
}
