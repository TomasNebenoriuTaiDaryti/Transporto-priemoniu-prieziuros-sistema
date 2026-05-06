package com.example.back.reminder.repo;

import com.example.back.reminder.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
public interface ReminderRepo extends JpaRepository<Reminder, Long> {
    Optional<Reminder> findByVehicleIdAndSourceTypeAndSourceId(Long vehicleId, String sourceType, String sourceId);
    void deleteByVehicleIdAndSourceTypeAndSourceId(Long vehicleId, String sourceType, String sourceId);
    List<Reminder> findAllByVehicleIdAndStatusInOrderByDueAtAscDueOdometerKmAsc(Long vehicleId, Collection<String> statuses);
}