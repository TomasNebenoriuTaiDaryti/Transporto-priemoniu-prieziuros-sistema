package com.example.back.document.service;

import com.example.back.document.domain.VehicleDocument;
import com.example.back.document.dto.DocumentDtos;
import com.example.back.reminder.service.ReminderService;
import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final VehicleDocumentRepo docs;
    private final VehicleAccessService access;
    private final ReminderService reminders;
    private final ObjectMapper om;

    public DocumentService(VehicleDocumentRepo docs, VehicleAccessService access, ReminderService reminders, ObjectMapper om) {
        this.docs = docs;
        this.access = access;
        this.reminders = reminders;
        this.om = om;
    }

    public List<DocumentDtos.DocumentResponse> list(Long userId, Long vehicleId) {
        var v = access.getVehicleOrThrow(vehicleId);
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        return docs.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .map(d -> toResponse(userId, v.getOwnerUserId(), d))
                .toList();
    }

    @Transactional
    public DocumentDtos.DocumentResponse addInsurance(Long userId, Long vehicleId, DocumentDtos.CreateInsuranceRequest req) {
        return createDoc(userId, vehicleId, DocumentDtos.DocumentType.INSURANCE,
                "Draudimas", null, req.startDate(), req.endDate(),
                jsonOf("price", req.price()));
    }

    @Transactional
    public DocumentDtos.DocumentResponse addInspection(Long userId, Long vehicleId, DocumentDtos.CreateInspectionRequest req) {
        return createDoc(userId, vehicleId, DocumentDtos.DocumentType.INSPECTION,
                "Techninė apžiūra", null, req.startDate(), req.endDate(), "{}");
    }

    @Transactional
    public DocumentDtos.DocumentResponse addFine(Long userId, Long vehicleId, DocumentDtos.CreateFineRequest req) {
        return createDoc(userId, vehicleId, DocumentDtos.DocumentType.FINE,
                "Bauda", req.reason(), req.date(), null,
                jsonOf("amount", req.amount(), "reason", req.reason()));
    }

    @Transactional
    public DocumentDtos.DocumentResponse addOther(Long userId, Long vehicleId, DocumentDtos.CreateOtherDocRequest req) {
        return createDoc(userId, vehicleId, DocumentDtos.DocumentType.OTHER,
                req.title(), req.description(), null, null, "{}");
    }

    @Transactional
    public DocumentDtos.DocumentResponse update(Long userId, UUID docId, DocumentDtos.UpdateDocRequest req) {
        VehicleDocument d = docs.findById(docId).orElseThrow(() -> new IllegalArgumentException("Dokumentas nerastas"));
        var v = access.getVehicleOrThrow(d.getVehicleId());
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        boolean canEdit = access.isVehicleOwner(userId, v) || (d.getUploadedByUserId() != null && d.getUploadedByUserId().equals(userId));
        if (!canEdit) throw new IllegalArgumentException("Negali redaguoti");

        d.setTitle(req.title());
        d.setDescription(req.description());
        d.setIssueDate(req.startDate());
        d.setExpiresAt(req.endDate());
        d.setMetaJson(req.metaJson() == null ? "{}" : req.metaJson());

        if (access.isVehicleOwner(userId, v)) {
            String sourceType = "DOCUMENT";
            String sourceId = d.getId().toString();

            if ("OTHER".equals(d.getType())) {
                reminders.deleteBySource(v.getId(), sourceType, sourceId);
            } else {
                if (d.getExpiresAt() != null) {
                    reminders.upsertDocumentReminder(v.getId(), sourceType, sourceId, d.getTitle(), d.getExpiresAt());
                } else {
                    reminders.deleteBySource(v.getId(), sourceType, sourceId);
                }
            }
        }

        return toResponse(userId, v.getOwnerUserId(), d);
    }

    @Transactional
    public void delete(Long userId, UUID docId) {
        VehicleDocument d = docs.findById(docId).orElseThrow(() -> new IllegalArgumentException("Dokumentas nerastas"));
        var v = access.getVehicleOrThrow(d.getVehicleId());
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        boolean canEdit = access.isVehicleOwner(userId, v) || (d.getUploadedByUserId() != null && d.getUploadedByUserId().equals(userId));
        if (!canEdit) throw new IllegalArgumentException("Negali trinti");
        
        if (access.isVehicleOwner(userId, v)) {
            reminders.deleteBySource(v.getId(), "DOCUMENT", d.getId().toString());
        }

        docs.delete(d);
    }

    private DocumentDtos.DocumentResponse createDoc(
            Long userId,
            Long vehicleId,
            DocumentDtos.DocumentType type,
            String title,
            String description,
            LocalDate start,
            LocalDate end,
            String metaJson
    ) {
        var v = access.getVehicleOrThrow(vehicleId);
        if (!access.canView(userId, v)) throw new IllegalArgumentException("Neturi prieigos");

        VehicleDocument d = new VehicleDocument();
        d.setId(UUID.randomUUID());
        d.setVehicleId(vehicleId);
        d.setUploadedByUserId(userId);
        d.setType(type.name());
        d.setTitle(title);
        d.setDescription(description);
        d.setIssueDate(start);
        d.setExpiresAt(end);
        d.setMetaJson(metaJson == null ? "{}" : metaJson);

        VehicleDocument saved = docs.save(d);

        if (access.isVehicleOwner(userId, v) && type != DocumentDtos.DocumentType.OTHER && end != null) {
            reminders.upsertDocumentReminder(vehicleId, "DOCUMENT", saved.getId().toString(), saved.getTitle(), end);
        }

        return toResponse(userId, v.getOwnerUserId(), saved);
    }

    private DocumentDtos.DocumentResponse toResponse(Long userId, Long vehicleOwnerId, VehicleDocument d) {
        boolean canEdit = vehicleOwnerId.equals(userId) || (d.getUploadedByUserId() != null && d.getUploadedByUserId().equals(userId));
        return new DocumentDtos.DocumentResponse(
                d.getId(),
                d.getVehicleId(),
                DocumentDtos.DocumentType.valueOf(d.getType()),
                d.getTitle(),
                d.getDescription(),
                d.getIssueDate(),
                d.getExpiresAt(),
                d.getMetaJson(),
                d.getUploadedByUserId(),
                canEdit
        );
    }

    private String jsonOf(Object... kv) {
        try {
            var node = om.createObjectNode();
            for (int i = 0; i < kv.length; i += 2) {
                String k = kv[i].toString();
                Object v = kv[i + 1];
                if (v == null) node.putNull(k);
                else if (v instanceof Number n) node.put(k, n.doubleValue());
                else node.put(k, v.toString());
            }
            return om.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}
