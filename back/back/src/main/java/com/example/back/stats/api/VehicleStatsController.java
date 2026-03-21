package com.example.back.stats.api;

import com.example.back.security.AuthUser;
import com.example.back.stats.dto.VehicleStatsResponse;
import com.example.back.stats.service.VehicleStatsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class VehicleStatsController {

    private final VehicleStatsService service;

    public VehicleStatsController(VehicleStatsService service) {
        this.service = service;
    }

    @GetMapping("/vehicles/{vehicleId}")
    public VehicleStatsResponse vehicleStats(@PathVariable Long vehicleId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.stats(userId, vehicleId);
    }
}
