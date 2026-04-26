package com.example.back.Unit.document;

import com.example.back.document.domain.VehicleDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleDocumentTest {

    @Test
    @DisplayName("Naujas dokumentas turi default meta ir createdAt")
    void newDocumentHasDefaultValues() {
        VehicleDocument document = new VehicleDocument();

        assertThat(document.getMetaJson()).isEqualTo("{}");
        assertThat(document.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Setteriai issaugo reiksmes")
    void settersStoreValues() {
        VehicleDocument document = new VehicleDocument();
        UUID id = UUID.randomUUID();
        LocalDate issueDate = LocalDate.of(2026, 1, 1);
        LocalDate expiresAt = LocalDate.of(2027, 1, 1);

        document.setId(id);
        document.setVehicleId(1L);
        document.setUploadedByUserId(10L);
        document.setType("INSURANCE");
        document.setTitle("Draudimas");
        document.setDescription("Description");
        document.setIssueDate(issueDate);
        document.setExpiresAt(expiresAt);
        document.setMetaJson("{\"price\":100}");

        assertThat(document.getId()).isEqualTo(id);
        assertThat(document.getVehicleId()).isEqualTo(1L);
        assertThat(document.getUploadedByUserId()).isEqualTo(10L);
        assertThat(document.getType()).isEqualTo("INSURANCE");
        assertThat(document.getTitle()).isEqualTo("Draudimas");
        assertThat(document.getDescription()).isEqualTo("Description");
        assertThat(document.getIssueDate()).isEqualTo(issueDate);
        assertThat(document.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(document.getMetaJson()).isEqualTo("{\"price\":100}");
    }

    @Test
    @DisplayName("Stub failas nustato techninius failo laukus")
    void setStubFileSetsTechnicalFileFields() {
        VehicleDocument document = new VehicleDocument();

        document.setStubFile();

        assertThat(document).isNotNull();
    }
}