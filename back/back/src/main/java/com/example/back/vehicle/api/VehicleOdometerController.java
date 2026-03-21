package com.example.back.vehicle.api;


import com.example.back.security.AuthUser;
import com.example.back.vehicle.dto.OdometerAppendRequest;
import com.example.back.vehicle.service.VehicleOdometerService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/odometer")
public class VehicleOdometerController {

    private final VehicleOdometerService service;

    public VehicleOdometerController(VehicleOdometerService service) {
        this.service = service;
    }

    @PostMapping("/append")
    public void append(@PathVariable Long vehicleId, @Valid @RequestBody OdometerAppendRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        service.appendKmIfActiveUser(userId, vehicleId, req.deltaKm());
    }
}
