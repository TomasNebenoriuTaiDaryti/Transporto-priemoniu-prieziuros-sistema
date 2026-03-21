package com.example.back.vehicle.repo;

import com.example.back.vehicle.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
public interface VehicleRepo extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByOwnerUserId(Long ownerUserId);
    List<Vehicle> findAllByGroupId(Long groupId);
    List<Vehicle> findAllByGroupIdIn(Collection<Long> groupIds);
    boolean existsByOwnerUserIdAndVin(Long ownerUserId, String vin);
    Optional<Vehicle> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    Optional<Vehicle> findById(Long id);
    @Modifying
    @Query(value = "update vehicle set odometer_km = coalesce(odometer_km, 0) + ?2 where id = ?1", nativeQuery = true)
    int addOdometerKm(Long vehicleId, Long deltaKm);
}
