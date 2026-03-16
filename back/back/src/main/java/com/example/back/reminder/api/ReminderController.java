package com.example.back.reminder.api;

import com.example.back.security.AuthUser;
import com.example.back.vehicle.service.VehicleAccessService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/reminders")
public class ReminderController {

    private final JdbcTemplate jdbc;
    private final VehicleAccessService access;

    public ReminderController(JdbcTemplate jdbc, VehicleAccessService access) {
        this.jdbc = jdbc;
        this.access = access;
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable Long vehicleId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        var v = access.getVehicleOrThrow(vehicleId);

        if (!access.isVehicleOwner(userId, v)) {
            return List.of();
        }

        return jdbc.queryForList("""
      select id, title, message, due_at, due_odometer_km, notify_before_days, notify_before_km, status
      from reminder
      where vehicle_id = ?
        and status in ('SCHEDULED','TRIGGERED')
      order by coalesce(due_at, now()) asc, coalesce(due_odometer_km, 9223372036854775807) asc
      """, vehicleId);
    }
}
