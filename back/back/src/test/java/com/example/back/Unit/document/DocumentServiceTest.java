package com.example.back.Unit.document;

import com.example.back.document.domain.VehicleDocument;
import com.example.back.document.dto.DocumentDtos;
import com.example.back.document.repo.VehicleDocumentRepo;
import com.example.back.document.service.DocumentService;
import com.example.back.reminder.service.ReminderPlanner;
import com.example.back.vehicle.domain.Vehicle;
import com.example.back.vehicle.service.VehicleAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private final VehicleDocumentRepo docs = mock(VehicleDocumentRepo.class);
    private final VehicleAccessService access = mock(VehicleAccessService.class);
    private final ReminderPlanner planner = mock(ReminderPlanner.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentService service = new DocumentService(docs, access, planner, objectMapper);

    @Test
    @DisplayName("Sarasas grazinamas kai vartotojas turi prieiga")
    void listReturnsDocumentsWhenUserCanView() {
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(UUID.randomUUID(), 1L, 20L, "INSURANCE", "Draudimas", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}");

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(docs.findAllByVehicleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(document));

        List<DocumentDtos.DocumentResponse> result = service.list(20L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(document.getId());
        assertThat(result.get(0).type()).isEqualTo(DocumentDtos.DocumentType.INSURANCE);
        assertThat(result.get(0).canEdit()).isTrue();
    }

    @Test
    @DisplayName("Sarasas meta klaida kai nera prieigos")
    void listThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle(1L, 10L);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.list(20L, 1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");

        verify(docs, never()).findAllByVehicleIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    @DisplayName("Prideda draudima ir sukuria priminima owner vartotojui")
    void addInsuranceCreatesDocumentAndReminderForOwner() {
        Vehicle vehicle = vehicle(1L, 10L);
        DocumentDtos.CreateInsuranceRequest request = new DocumentDtos.CreateInsuranceRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 100.0);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);
        when(docs.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentDtos.DocumentResponse result = service.addInsurance(10L, 1L, request);

        assertThat(result.type()).isEqualTo(DocumentDtos.DocumentType.INSURANCE);
        assertThat(result.title()).isEqualTo("Draudimas");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(result.metaJson()).contains("\"price\":100.0");
        assertThat(result.canEdit()).isTrue();
        verify(planner).upsertDocumentReminder(eq(1L), eq("DOCUMENT"), anyString(), eq("Draudimas"), eq(LocalDate.of(2027, 1, 1)));
    }

    @Test
    @DisplayName("Prideda technine ir sukuria priminima")
    void addInspectionCreatesDocumentAndReminder() {
        Vehicle vehicle = vehicle(1L, 10L);
        DocumentDtos.CreateInspectionRequest request = new DocumentDtos.CreateInspectionRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);
        when(docs.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentDtos.DocumentResponse result = service.addInspection(10L, 1L, request);

        assertThat(result.type()).isEqualTo(DocumentDtos.DocumentType.INSPECTION);
        assertThat(result.title()).isEqualTo("Techninė apžiūra");
        assertThat(result.metaJson()).isEqualTo("{}");
        verify(planner).upsertDocumentReminder(eq(1L), eq("DOCUMENT"), anyString(), eq("Techninė apžiūra"), eq(LocalDate.of(2027, 1, 1)));
    }

    @Test
    @DisplayName("Prideda bauda be priminimo kai nera end datos")
    void addFineCreatesDocumentWithoutReminder() {
        Vehicle vehicle = vehicle(1L, 10L);
        DocumentDtos.CreateFineRequest request = new DocumentDtos.CreateFineRequest(LocalDate.of(2026, 1, 1), 50.0, "Speed");

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);
        when(docs.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentDtos.DocumentResponse result = service.addFine(10L, 1L, request);

        assertThat(result.type()).isEqualTo(DocumentDtos.DocumentType.FINE);
        assertThat(result.title()).isEqualTo("Bauda");
        assertThat(result.description()).isEqualTo("Speed");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.endDate()).isNull();
        assertThat(result.metaJson()).contains("\"amount\":50.0");
        assertThat(result.metaJson()).contains("\"reason\":\"Speed\"");
        verifyNoInteractions(planner);
    }

    @Test
    @DisplayName("Prideda kita dokumenta be priminimo")
    void addOtherCreatesDocumentWithoutReminder() {
        Vehicle vehicle = vehicle(1L, 10L);
        DocumentDtos.CreateOtherDocRequest request = new DocumentDtos.CreateOtherDocRequest("Other", "Desc", LocalDate.of(2027, 1, 1));

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);
        when(docs.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentDtos.DocumentResponse result = service.addOther(10L, 1L, request);

        assertThat(result.type()).isEqualTo(DocumentDtos.DocumentType.OTHER);
        assertThat(result.title()).isEqualTo("Other");
        assertThat(result.description()).isEqualTo("Desc");
        assertThat(result.startDate()).isNull();
        assertThat(result.endDate()).isNull();
        verifyNoInteractions(planner);
    }

    @Test
    @DisplayName("Sukurimas meta klaida kai nera prieigos")
    void createThrowsWhenUserCannotView() {
        Vehicle vehicle = vehicle(1L, 10L);
        DocumentDtos.CreateInsuranceRequest request = new DocumentDtos.CreateInsuranceRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 100.0);

        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.addInsurance(20L, 1L, request)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");

        verify(docs, never()).save(any());
    }

    @Test
    @DisplayName("Atnaujina dokumenta kai vartotojas owner ir end data yra")
    void updateWorksForOwnerAndUpsertsReminderWhenEndDateExists() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}");
        DocumentDtos.UpdateDocRequest request = new DocumentDtos.UpdateDocRequest("New", "New desc", LocalDate.of(2026, 2, 1), LocalDate.of(2027, 2, 1), "{\"price\":120}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);

        DocumentDtos.DocumentResponse result = service.update(10L, docId, request);

        assertThat(result.title()).isEqualTo("New");
        assertThat(result.description()).isEqualTo("New desc");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(result.metaJson()).isEqualTo("{\"price\":120}");
        verify(planner).upsertDocumentReminder(1L, "DOCUMENT", docId.toString(), "New", LocalDate.of(2027, 2, 1));
    }

    @Test
    @DisplayName("Atnaujinant OTHER dokumenta owner istrina priminima")
    void updateOtherDocumentDeletesReminderForOwner() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "OTHER", "Other", null, null, "{}");
        DocumentDtos.UpdateDocRequest request = new DocumentDtos.UpdateDocRequest("Other new", "Desc", null, LocalDate.of(2027, 1, 1), null);

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);

        DocumentDtos.DocumentResponse result = service.update(10L, docId, request);

        assertThat(result.metaJson()).isEqualTo("{}");
        verify(planner).deleteBySource(1L, "DOCUMENT", docId.toString());
        verify(planner, never()).upsertDocumentReminder(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Atnaujinant ne OTHER be end datos istrina priminima")
    void updateNonOtherWithoutEndDateDeletesReminder() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Draudimas", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}");
        DocumentDtos.UpdateDocRequest request = new DocumentDtos.UpdateDocRequest("Draudimas", null, LocalDate.of(2026, 1, 1), null, null);

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);

        service.update(10L, docId, request);

        verify(planner).deleteBySource(1L, "DOCUMENT", docId.toString());
    }

    @Test
    @DisplayName("Atnaujinimas leidziamas ikelusiam vartotojui bet be owner reminderio")
    void updateWorksForUploaderWithoutOwnerReminderActions() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}");
        DocumentDtos.UpdateDocRequest request = new DocumentDtos.UpdateDocRequest("New", "Desc", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null);

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(20L, vehicle)).thenReturn(false);

        DocumentDtos.DocumentResponse result = service.update(20L, docId, request);

        assertThat(result.canEdit()).isTrue();
        verifyNoInteractions(planner);
    }

    @Test
    @DisplayName("Atnaujinimas meta klaida kai dokumentas nerastas")
    void updateThrowsWhenDocumentMissing() {
        UUID docId = UUID.randomUUID();
        when(docs.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, docId, new DocumentDtos.UpdateDocRequest("Title", null, null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Dokumentas nerastas");
    }

    @Test
    @DisplayName("Atnaujinimas meta klaida kai nera prieigos")
    void updateThrowsWhenUserCannotView() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", null, null, "{}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.update(30L, docId, new DocumentDtos.UpdateDocRequest("Title", null, null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");
    }

    @Test
    @DisplayName("Atnaujinimas meta klaida kai negali redaguoti")
    void updateThrowsWhenUserCannotEdit() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", null, null, "{}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.update(30L, docId, new DocumentDtos.UpdateDocRequest("Title", null, null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage("Negali redaguoti");
    }

    @Test
    @DisplayName("Istrina dokumenta kai vartotojas owner")
    void deleteWorksForOwnerAndDeletesReminder() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", null, null, "{}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(10L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(10L, vehicle)).thenReturn(true);

        service.delete(10L, docId);

        verify(planner).deleteBySource(1L, "DOCUMENT", docId.toString());
        verify(docs).delete(document);
    }

    @Test
    @DisplayName("Istrina dokumenta kai vartotojas yra ikeles")
    void deleteWorksForUploaderWithoutDeletingReminder() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", null, null, "{}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(20L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(20L, vehicle)).thenReturn(false);

        service.delete(20L, docId);

        verifyNoInteractions(planner);
        verify(docs).delete(document);
    }

    @Test
    @DisplayName("Trynimas meta klaida kai dokumentas nerastas")
    void deleteThrowsWhenDocumentMissing() {
        UUID docId = UUID.randomUUID();
        when(docs.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, docId)).isInstanceOf(IllegalArgumentException.class).hasMessage("Dokumentas nerastas");
    }

    @Test
    @DisplayName("Trynimas meta klaida kai nera prieigos")
    void deleteThrowsWhenUserCannotView() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", null, null, "{}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(30L, docId)).isInstanceOf(IllegalArgumentException.class).hasMessage("Neturi prieigos");
    }

    @Test
    @DisplayName("Trynimas meta klaida kai negali trinti")
    void deleteThrowsWhenUserCannotEdit() {
        UUID docId = UUID.randomUUID();
        Vehicle vehicle = vehicle(1L, 10L);
        VehicleDocument document = document(docId, 1L, 20L, "INSURANCE", "Old", null, null, "{}");

        when(docs.findById(docId)).thenReturn(Optional.of(document));
        when(access.getVehicleOrThrow(1L)).thenReturn(vehicle);
        when(access.canView(30L, vehicle)).thenReturn(true);
        when(access.isVehicleOwner(30L, vehicle)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(30L, docId)).isInstanceOf(IllegalArgumentException.class).hasMessage("Negali trinti");
    }

    private static Vehicle vehicle(Long id, Long ownerUserId) {
        Vehicle vehicle = new Vehicle(ownerUserId);
        vehicle.setId(id);
        return vehicle;
    }

    private static VehicleDocument document(UUID id, Long vehicleId, Long uploadedByUserId, String type, String title, LocalDate issueDate, LocalDate expiresAt, String metaJson) {
        VehicleDocument document = new VehicleDocument();
        document.setId(id);
        document.setVehicleId(vehicleId);
        document.setUploadedByUserId(uploadedByUserId);
        document.setType(type);
        document.setTitle(title);
        document.setDescription("Description");
        document.setIssueDate(issueDate);
        document.setExpiresAt(expiresAt);
        document.setMetaJson(metaJson);
        document.setStubFile();
        return document;
    }
}