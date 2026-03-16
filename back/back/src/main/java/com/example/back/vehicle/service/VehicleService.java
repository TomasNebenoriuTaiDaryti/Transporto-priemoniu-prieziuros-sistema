package com.example.back.vehicle.service;

import com.example.back.auth.repo.AccountRepo;
import com.example.back.group.repo.*;
import com.example.back.vehicle.domain.ActiveVehicle;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.dto.*;
import com.example.back.vehicle.repo.ActiveVehicleRepo;
import com.example.back.vehicle.repo.VehicleRepo;
import com.example.back.vehicle.vin.VincarioClient;
import com.example.back.vehicle.vin.dto.VincarioDecodeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class VehicleService {

    private final VehicleRepo vehicles;
    private final VincarioClient vincario;
    private final GroupMemberRepo groupMembers;
    private final ActiveVehicleRepo activeVehicles;
    private final AccountRepo users;

    public VehicleService(
            VehicleRepo vehicles,
            VincarioClient vincario,
            GroupMemberRepo groupMembers,
            ActiveVehicleRepo activeVehicles,
            AccountRepo users
    ) {
        this.vehicles = vehicles;
        this.vincario = vincario;
        this.groupMembers = groupMembers;
        this.activeVehicles = activeVehicles;
        this.users = users;
    }

    public VehiclePreviewResponse previewByVin(Long ownerUserId, String vin) {
        String vinUpper = normalizeVinRequired(vin);

        if (vehicles.existsByOwnerUserIdAndVin(ownerUserId, vinUpper)) {
            throw new IllegalArgumentException("Šis VIN jau naudojamas tavo garaže");
        }

        VincarioDecodeResponse d = vincario.decode(vinUpper);
        String rawFuel = d.fuelType();
        String dbFuel = FuelTypeMapper.normalizeToDbCode(rawFuel);

        return new VehiclePreviewResponse(
                vinUpper,
                d.make(),
                d.model(),
                d.modelYear(),
                d.engineDisplacementCc(),
                d.transmission(),
                rawFuel,
                dbFuel,
                d.drive(),
                d.body(),
                d.doors(),
                d.seats(),
                d.co2Gkm(),
                d.plantCountry(),
                d.manufacturer()
        );
    }

    @Transactional
    public VehicleResponse confirmCreatePersonal(Long ownerUserId, VehicleConfirmRequest req) {
        String vinUpper = normalizeVinOptional(req.vin());

        if (vinUpper != null && vehicles.existsByOwnerUserIdAndVin(ownerUserId, vinUpper)) {
            throw new IllegalArgumentException("Šis VIN jau naudojamas tavo garaže");
        }

        Vehicle v = new Vehicle(ownerUserId);
        v.setVin(vinUpper);
        v.setGroupId(null);

        applyConfirm(v, req);

        Vehicle saved = vehicles.save(v);
        return toResponse(saved);
    }

    public List<VehicleResponse> listMyVehicles(Long ownerUserId) {
        return vehicles.findAllByOwnerUserId(ownerUserId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VehicleResponse updateMyVehicle(Long ownerUserId, Long vehicleId, VehicleUpdateRequest req) {
        Vehicle v = vehicles.findByIdAndOwnerUserId(vehicleId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));

        v.setMake(req.make());
        v.setModel(req.model());
        v.setModelYear(req.modelYear());
        v.setEngineDisplacementCc(req.engineDisplacementCc());
        v.setFuelType(req.fuelType());
        v.setOdometerKm(req.odometerKm());
        v.setTransmission(req.transmission());
        v.setDrive(req.drive());
        v.setBody(req.body());
        v.setDoors(req.doors());
        v.setSeats(req.seats());
        v.setCo2Gkm(BigDecimal.valueOf(req.co2Gkm()));
        v.setPlantCountry(req.plantCountry());
        v.setManufacturer(req.manufacturer());

        return toResponse(v);
    }

    @Transactional
    public void deleteMyVehicle(Long ownerUserId, Long vehicleId) {
        Vehicle v = vehicles.findByIdAndOwnerUserId(vehicleId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));

        if (v.getGroupId() != null) {
            throw new IllegalArgumentException("Negalima ištrinti, kol mašina yra grupėje. Pirma pašalink iš grupės.");
        }
        activeVehicles.findByVehicleId(vehicleId).ifPresent(av -> activeVehicles.deleteById(av.getUserId()));

        vehicles.delete(v);
    }

    @Transactional
    public VehicleResponse assignToGroup(Long ownerUserId, Long vehicleId, Long groupId) {
        Vehicle v = vehicles.findByIdAndOwnerUserId(vehicleId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));

        if (v.getGroupId() != null) {
            throw new IllegalArgumentException("Ši mašina jau priskirta grupei. Pirma pašalink iš grupės.");
        }
        v.setGroupId(groupId);
        return toResponse(v);
    }

    @Transactional
    public VehicleResponse removeFromGroup(Long ownerUserId, Long vehicleId, Long groupId) {
        Vehicle v = vehicles.findByIdAndOwnerUserId(vehicleId, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));

        if (v.getGroupId() == null || !v.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("Mašina nėra šioje grupėje");
        }
        v.setGroupId(null);
        return toResponse(v);
    }

    public List<VehicleResponse> listGroupVehicles(Long groupId) {
        return vehicles.findAllByGroupId(groupId).stream().map(this::toResponse).toList();
    }

    public List<VehicleListItemResponse> listAccessibleVehicles(Long userId) {
        List<Vehicle> owned = vehicles.findAllByOwnerUserId(userId);
        var memberships = groupMembers.findAllByUserId(userId);
        Set<Long> groupIds = new HashSet<>();
        for (var m : memberships) groupIds.add(m.getGroupId());

        List<Vehicle> groupVehicles = groupIds.isEmpty() ? List.of() : vehicles.findAllByGroupIdIn(groupIds);
        Map<Long, Vehicle> merged = new LinkedHashMap<>();
        for (var v : owned) merged.put(v.getId(), v);
        for (var v : groupVehicles) merged.putIfAbsent(v.getId(), v);
        var myActive = activeVehicles.findByUserId(userId).map(ActiveVehicle::getVehicleId).orElse(null);

        Map<Long, String> activeByEmail = new HashMap<>();
        for (var av : activeVehicles.findAll()) {
            var email = users.findById(av.getUserId()).map(u -> u.getEmail()).orElse("unknown");
            activeByEmail.put(av.getVehicleId(), email);
        }

        Map<Long, String> ownerEmailCache = new HashMap<>();
        List<VehicleListItemResponse> out = new ArrayList<>();
        for (var v : merged.values()) {
            Long ownerId = v.getOwnerUserId();
            String ownerEmail = ownerEmailCache.computeIfAbsent(ownerId,
                    id -> users.findById(id).map(u -> u.getEmail()).orElse("unknown"));

            boolean canEdit = Objects.equals(ownerId, userId);
            boolean canDelete = canEdit && v.getGroupId() == null;

            boolean activeByMe = myActive != null && Objects.equals(myActive, v.getId());
            String usedBy = activeByEmail.get(v.getId());

            out.add(new VehicleListItemResponse(
                    v.getId(),
                    v.getVin(),
                    v.getMake(),
                    v.getModel(),
                    v.getModelYear(),
                    v.getFuelType(),
                    v.getOdometerKm(),
                    v.getGroupId(),
                    ownerId,
                    ownerEmail,
                    canEdit,
                    canDelete,
                    activeByMe,
                    usedBy
            ));
        }

        return out;
    }

    @Transactional
    public void setActiveVehicle(Long userId, Long vehicleId) {
        Vehicle v = vehicles.findById(vehicleId).orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));

        boolean canAccess = Objects.equals(v.getOwnerUserId(), userId);
        if (!canAccess && v.getGroupId() != null) {
            canAccess = groupMembers.existsByGroupIdAndUserId(v.getGroupId(), userId);
        }
        if (!canAccess) throw new IllegalArgumentException("Neturi prieigos prie šios mašinos");

        var used = activeVehicles.findByVehicleId(vehicleId);
        if (used.isPresent() && !Objects.equals(used.get().getUserId(), userId)) {
            throw new IllegalArgumentException("Šią mašiną jau naudoja kitas vartotojas");
        }

        activeVehicles.deleteByUserId(userId);

        activeVehicles.save(new ActiveVehicle(userId, vehicleId));
    }

    @Transactional
    public void clearActiveVehicle(Long userId) {
        activeVehicles.deleteByUserId(userId);
    }

    private void applyConfirm(Vehicle v, VehicleConfirmRequest req) {
        v.setMake(req.make());
        v.setModel(req.model());
        v.setModelYear(req.modelYear());
        v.setEngineDisplacementCc(req.engineDisplacementCc());
        v.setFuelType(req.fuelType());
        v.setOdometerKm(req.odometerKm());
        v.setTransmission(req.transmission());
        v.setDrive(req.drive());
        v.setBody(req.body());
        v.setDoors(req.doors());
        v.setSeats(req.seats());
        v.setCo2Gkm(req.co2Gkm());
        v.setPlantCountry(req.plantCountry());
        v.setManufacturer(req.manufacturer());
    }

    private VehicleResponse toResponse(Vehicle v) {
        return new VehicleResponse(
                v.getId(),
                v.getVin(),
                v.getMake(),
                v.getModel(),
                v.getModelYear(),
                v.getFuelType(),
                v.getOdometerKm(),
                v.getGroupId()
        );
    }

    private VehicleDetailsResponse toDetails(Vehicle v, boolean isOwner) {
        return new VehicleDetailsResponse(
                v.getId(),
                v.getVin(),
                v.getMake(),
                v.getModel(),
                v.getModelYear(),
                v.getEngineDisplacementCc(),
                v.getFuelType(),
                v.getOdometerKm(),
                v.getTransmission(),
                v.getDrive(),
                v.getBody(),
                v.getDoors(),
                v.getSeats(),
                v.getCo2Gkm(),
                v.getPlantCountry(),
                v.getManufacturer(),
                v.getGroupId(),
                isOwner
        );
    }

    private String normalizeVinRequired(String vin) {
        String x = vin.trim().toUpperCase();
        if (x.length() != 17) throw new IllegalArgumentException("VIN turi būti 17 simbolių");
        return x;
    }

    private String normalizeVinOptional(String vin) {
        if (vin == null) return null;
        String x = vin.trim();
        if (x.isEmpty()) return null;
        x = x.toUpperCase();
        if (x.length() != 17) throw new IllegalArgumentException("VIN turi būti 17 simbolių");
        return x;
    }

    public VehicleDetailsResponse getVehicleDetailsForViewer(Long userId, Long vehicleId) {
        Vehicle v = vehicles.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Mašina nerasta"));

        boolean canView = Objects.equals(v.getOwnerUserId(), userId);
        if (!canView && v.getGroupId() != null) {
            canView = groupMembers.existsByGroupIdAndUserId(v.getGroupId(), userId);
        }
        if (!canView) throw new IllegalArgumentException("Neturi prieigos prie šios mašinos");

        boolean isOwner = Objects.equals(v.getOwnerUserId(), userId);
        return toDetails(v, isOwner);
    }
}