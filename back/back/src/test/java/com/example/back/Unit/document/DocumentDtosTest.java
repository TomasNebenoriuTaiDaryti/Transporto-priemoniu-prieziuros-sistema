package com.example.back.Unit.document;

import com.example.back.document.dto.DocumentDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentDtosTest {

    @Test
    @DisplayName("DocumentType turi visas reiksmes")
    void documentTypeHasAllValues() {
        assertThat(Arrays.asList(DocumentDtos.DocumentType.values())).containsExactly(DocumentDtos.DocumentType.INSURANCE, DocumentDtos.DocumentType.INSPECTION, DocumentDtos.DocumentType.FINE, DocumentDtos.DocumentType.OTHER);
        assertThat(DocumentDtos.DocumentType.valueOf("INSURANCE")).isEqualTo(DocumentDtos.DocumentType.INSURANCE);
    }

    @Test
    @DisplayName("DocumentResponse issaugo laukus")
    void documentResponseStoresFields() {
        UUID id = UUID.randomUUID();
        DocumentDtos.DocumentResponse response = new DocumentDtos.DocumentResponse(id, 1L, DocumentDtos.DocumentType.INSURANCE, "Draudimas", "Desc", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}", 10L, true);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(DocumentDtos.DocumentType.INSURANCE);
        assertThat(response.title()).isEqualTo("Draudimas");
        assertThat(response.description()).isEqualTo("Desc");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(response.metaJson()).isEqualTo("{}");
        assertThat(response.createdByUserId()).isEqualTo(10L);
        assertThat(response.canEdit()).isTrue();
    }

    @Test
    @DisplayName("CreateInsuranceRequest issaugo laukus")
    void createInsuranceRequestStoresFields() {
        DocumentDtos.CreateInsuranceRequest request = new DocumentDtos.CreateInsuranceRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 100.0);

        assertThat(request.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(request.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(request.price()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("CreateInspectionRequest issaugo laukus")
    void createInspectionRequestStoresFields() {
        DocumentDtos.CreateInspectionRequest request = new DocumentDtos.CreateInspectionRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        assertThat(request.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(request.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("CreateFineRequest issaugo laukus")
    void createFineRequestStoresFields() {
        DocumentDtos.CreateFineRequest request = new DocumentDtos.CreateFineRequest(LocalDate.of(2026, 1, 1), 50.0, "Speed");

        assertThat(request.date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(request.amount()).isEqualTo(50.0);
        assertThat(request.reason()).isEqualTo("Speed");
    }

    @Test
    @DisplayName("CreateOtherDocRequest issaugo laukus")
    void createOtherDocRequestStoresFields() {
        DocumentDtos.CreateOtherDocRequest request = new DocumentDtos.CreateOtherDocRequest("Other", "Desc", LocalDate.of(2027, 1, 1));

        assertThat(request.title()).isEqualTo("Other");
        assertThat(request.description()).isEqualTo("Desc");
        assertThat(request.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("UpdateDocRequest issaugo laukus")
    void updateDocRequestStoresFields() {
        DocumentDtos.UpdateDocRequest request = new DocumentDtos.UpdateDocRequest("Title", "Desc", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}");

        assertThat(request.title()).isEqualTo("Title");
        assertThat(request.description()).isEqualTo("Desc");
        assertThat(request.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(request.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(request.metaJson()).isEqualTo("{}");
    }

    @Test
    @DisplayName("Privatus konstruktorius gali buti iskviestas refleksija")
    void privateConstructorCanBeCalledByReflection() throws Exception {
        Constructor<DocumentDtos> constructor = DocumentDtos.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        DocumentDtos result = constructor.newInstance();

        assertThat(result).isNotNull();
    }
}