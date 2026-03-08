package com.example.back.vehicle.api;

import com.example.back.security.AuthUser;
import com.example.back.vehicle.dto.*;
import com.example.back.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public VehiclePreviewResponse preview(@Valid @RequestBody VehiclePreviewRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.previewByVin(userId, req.vin());
    }

    @PostMapping
    public VehicleResponse createPersonal(@Valid @RequestBody VehicleConfirmRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.confirmCreatePersonal(userId, req);
    }

    @GetMapping("/accessible")
    public List<VehicleListItemResponse> accessible(Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.listAccessibleVehicles(userId);
    }

    @GetMapping("/my")
    public List<VehicleResponse> my(Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.listMyVehicles(userId);
    }

    @GetMapping("/{id}")
    public VehicleDetailsResponse details(@PathVariable Long id, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.getMyVehicleDetails(userId, id);
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.updateMyVehicle(userId, id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        service.deleteMyVehicle(userId, id);
    }

    @GetMapping("/group/{groupId}")
    public List<VehicleResponse> groupVehicles(@PathVariable Long groupId) {
        return service.listGroupVehicles(groupId);
    }

    @PostMapping("/{id}/use")
    public void use(@PathVariable Long id, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        service.setActiveVehicle(userId, id);
    }

    @PostMapping("/unuse")
    public void unuse(Authentication auth) {
        Long userId = AuthUser.userId(auth);
        service.clearActiveVehicle(userId);
    }
}