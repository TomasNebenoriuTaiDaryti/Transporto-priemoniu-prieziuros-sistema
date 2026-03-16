package com.example.back.document.repo;

import com.example.back.document.domain.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
public interface VehicleDocumentRepo extends JpaRepository<VehicleDocument, UUID> {
    List<VehicleDocument> findAllByVehicleIdOrderByCreatedAtDesc(Long vehicleId);
}
