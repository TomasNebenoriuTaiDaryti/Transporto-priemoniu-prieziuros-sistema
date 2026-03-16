package com.example.back.record.repo;

import com.example.back.record.domain.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRecordRepo extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findAllByVehicleIdOrderByPerformedAtDesc(Long vehicleId);
}
