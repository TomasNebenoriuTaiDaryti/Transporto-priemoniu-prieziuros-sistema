package com.example.back.record.service;

import com.example.back.record.domain.ServiceRecord;
import com.example.back.record.dto.RecordDtos;
import com.example.back.record.repo.ServiceRecordRepo;
import com.example.back.reminder.service.ReminderPlanner;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class RecordService {

    private final ServiceRecordRepo records;
    private final VehicleAccessService access;
    private final ReminderPlanner planner;
    private final ObjectMapper om;

    public RecordService(ServiceRecordRepo records, VehicleAccessService access, ReminderPlanner planner, ObjectMapper om) {
        this.records = records;
        this.access = access;
        this.planner = planner;
        this.om = om;
    }

    public List<RecordDtos.RecordResponse> list(Long userId, Long vehicleId) {
        var v = access.getVehicleOrThrow(vehicleId);
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        return records.findAllByVehicleIdOrderByPerformedAtDesc(vehicleId).stream()
                .map(r -> toResponse(userId, v.getOwnerUserId(), r))
                .toList();
    }

    @Transactional
    public RecordDtos.RecordResponse create(Long userId, Long vehicleId, RecordDtos.CreateRecordRequest req) {
        var v = access.getVehicleOrThrow(vehicleId);
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        ServiceRecord r = new ServiceRecord();
        r.setVehicleId(vehicleId);
        r.setCreatedByUserId(userId);

        Instant performed = req.performedAt() != null ? req.performedAt() : Instant.now();
        Long odo = req.odometerKm() != null ? req.odometerKm() : v.getOdometerKm();

        r.setPerformedAt(performed);
        r.setOdometerKm(odo);

        r.setKind(req.kind().name());
        r.setType("MAINTENANCE");

        String title = switch (req.kind()) {
            case OIL -> "Tepalų ir filtro keitimas";
            case TIRES -> "Padangų keitimas";
            case BRAKES -> "Stabdžių aptarnavimas";
            case SERVICE -> "Apsilankymas servise";
            case FUEL -> "Degalų pildymas";
            case OTHER -> (req.title() == null || req.title().isBlank()) ? "Kitas įrašas" : req.title();
        };

        r.setTitle(title);
        r.setDescription(req.description());
        r.setTotalCost(req.totalCost());
        r.setCurrency(req.currency());

        var meta = om.createObjectNode();
        if (req.tireType() != null) meta.put("tireType", req.tireType());
        if (req.tireAgeYears() != null) meta.put("tireAgeYears", req.tireAgeYears());
        if (req.liters() != null) meta.put("liters", req.liters());
        r.setMetaJson(meta.toString());

        ServiceRecord saved = records.save(r);

        applyRecordReminders(vehicleId, saved.getId(), req.kind(), odo, performed, req.tireAgeYears());

        return toResponse(userId, v.getOwnerUserId(), saved);
    }

    @Transactional
    public RecordDtos.RecordResponse update(Long userId, Long recordId, RecordDtos.UpdateRecordRequest req) {
        ServiceRecord r = records.findById(recordId).orElseThrow(() -> new IllegalArgumentException("Įrašas nerastas"));
        var v = access.getVehicleOrThrow(r.getVehicleId());
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        boolean canEdit = access.isVehicleOwner(userId, v) || (r.getCreatedByUserId() != null && r.getCreatedByUserId().equals(userId));
        if (!canEdit) throw new IllegalArgumentException("Negali redaguoti");

        r.setKind(req.kind().name());
        r.setPerformedAt(req.performedAt());
        r.setOdometerKm(req.odometerKm());
        r.setDescription(req.description());
        r.setCurrency(req.currency());
        r.setTotalCost(req.totalCost());

        String title = switch (req.kind()) {
            case OIL -> "Tepalų ir filtro keitimas";
            case TIRES -> "Padangų keitimas";
            case BRAKES -> "Stabdžių aptarnavimas";
            case SERVICE -> "Apsilankymas servise";
            case FUEL -> "Degalų pildymas";
            case OTHER -> (req.title() == null || req.title().isBlank()) ? "Kitas įrašas" : req.title();
        };
        r.setTitle(title);

        var meta = om.createObjectNode();
        if (req.tireType() != null) meta.put("tireType", req.tireType());
        if (req.tireAgeYears() != null) meta.put("tireAgeYears", req.tireAgeYears());
        r.setMetaJson(meta.toString());

        planner.deleteBySource(v.getId(), "RECORD_OIL_KM", r.getId().toString());
        planner.deleteBySource(v.getId(), "RECORD_OIL_DATE", r.getId().toString());
        planner.deleteBySource(v.getId(), "RECORD_BRAKES_KM", r.getId().toString());
        planner.deleteBySource(v.getId(), "RECORD_TIRES_DATE", r.getId().toString());

        applyRecordReminders(v.getId(), r.getId(), req.kind(), req.odometerKm(), req.performedAt(), req.tireAgeYears());

        return toResponse(userId, v.getOwnerUserId(), r);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        ServiceRecord r = records.findById(recordId).orElseThrow(() -> new IllegalArgumentException("Įrašas nerastas"));
        var v = access.getVehicleOrThrow(r.getVehicleId());
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        boolean canEdit = access.isVehicleOwner(userId, v) || (r.getCreatedByUserId() != null && r.getCreatedByUserId().equals(userId));
        if (!canEdit) throw new IllegalArgumentException("Negali trinti");

        planner.deleteBySource(v.getId(), "RECORD_OIL_KM", r.getId().toString());
        planner.deleteBySource(v.getId(), "RECORD_OIL_DATE", r.getId().toString());
        planner.deleteBySource(v.getId(), "RECORD_BRAKES_KM", r.getId().toString());
        planner.deleteBySource(v.getId(), "RECORD_TIRES_DATE", r.getId().toString());

        records.delete(r);
    }

    private void applyRecordReminders(Long vehicleId, Long recordId, RecordDtos.RecordKind kind, Long odo, Instant performedAt, Integer tireAgeYears) {
        if (kind == RecordDtos.RecordKind.OIL) {
            if (odo != null) {
                planner.upsertMileageReminder(vehicleId, "RECORD_OIL_KM", recordId.toString(), "Tepalų keitimas (rida)", odo + 10000);
            }
            LocalDate dueDate = LocalDate.ofInstant(performedAt, ZoneId.of("Europe/Vilnius")).plusYears(1);
            planner.upsertDateReminder(vehicleId, "RECORD_OIL_DATE", recordId.toString(), "Tepalų keitimas (laikas)", dueDate);
        }

        if (kind == RecordDtos.RecordKind.BRAKES && odo != null) {
            planner.upsertMileageReminder(vehicleId, "RECORD_BRAKES_KM", recordId.toString(), "Stabdžiai", odo + 40000);
        }

        if (kind == RecordDtos.RecordKind.TIRES) {
            int age = (tireAgeYears == null) ? 0 : Math.max(0, tireAgeYears);
            int left = Math.max(0, 6 - age);
            LocalDate dueDate = LocalDate.now(ZoneId.of("Europe/Vilnius")).plusYears(left);
            planner.upsertDateReminder(vehicleId, "RECORD_TIRES_DATE", recordId.toString(), "Padangos (amžius)", dueDate);
        }
    }

    private RecordDtos.RecordResponse toResponse(Long userId, Long vehicleOwnerId, ServiceRecord r) {
        boolean canEdit = vehicleOwnerId.equals(userId) || (r.getCreatedByUserId() != null && r.getCreatedByUserId().equals(userId));
        return new RecordDtos.RecordResponse(
                r.getId(),
                r.getVehicleId(),
                RecordDtos.RecordKind.valueOf(r.getKind()),
                r.getTitle(),
                r.getDescription(),
                r.getPerformedAt(),
                r.getOdometerKm(),
                r.getTotalCost(),
                r.getCurrency(),
                r.getMetaJson(),
                r.getCreatedByUserId(),
                canEdit
        );
    }
}
