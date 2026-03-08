package com.example.back.vehicle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "active_vehicle")
public class ActiveVehicle {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "vehicle_id", nullable = false, unique = true)
    private Long vehicleId;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt = Instant.now();

    protected ActiveVehicle() {}

    public ActiveVehicle(Long userId, Long vehicleId) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.activatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public Long getVehicleId() { return vehicleId; }
    public Instant getActivatedAt() { return activatedAt; }

    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
}
