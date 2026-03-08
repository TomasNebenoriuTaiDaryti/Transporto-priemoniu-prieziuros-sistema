package com.example.back.vehicle.repo;

import com.example.back.vehicle.domain.ActiveVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
public interface ActiveVehicleRepo extends JpaRepository<ActiveVehicle, Long> {
    Optional<ActiveVehicle> findByUserId(Long userId);
    Optional<ActiveVehicle> findByVehicleId(Long vehicleId);
    void deleteByUserId(Long userId);
    void deleteByVehicleId(Long vehicleId);
    void deleteByVehicleIdIn(Collection<Long> vehicleIds);
}
