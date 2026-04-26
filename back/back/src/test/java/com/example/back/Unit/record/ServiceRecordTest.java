package com.example.back.Unit.record;

import com.example.back.record.domain.ServiceRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRecordTest {

    @Test
    @DisplayName("Naujas irasas turi default reiksmes")
    void newRecordHasDefaultValues() {
        ServiceRecord record = new ServiceRecord();

        assertThat(record.getTotalCost()).isEqualTo(BigDecimal.ZERO);
        assertThat(record.getCurrency()).isEqualTo("EUR");
        assertThat(record.getMetaJson()).isEqualTo("{}");
    }

    @Test
    @DisplayName("Setteriai issaugo reiksmes")
    void settersStoreValues() {
        ServiceRecord record = new ServiceRecord();
        Instant performedAt = Instant.parse("2026-01-01T10:00:00Z");

        record.setVehicleId(1L);
        record.setCreatedByUserId(10L);
        record.setType("MAINTENANCE");
        record.setTitle("Title");
        record.setDescription("Description");
        record.setPerformedAt(performedAt);
        record.setOdometerKm(1000L);
        record.setKind("OIL");
        record.setMetaJson("{\"liters\":25}");
        record.setCurrency("USD");
        record.setTotalCost(new BigDecimal("50.00"));

        assertThat(record.getVehicleId()).isEqualTo(1L);
        assertThat(record.getCreatedByUserId()).isEqualTo(10L);
        assertThat(record.getType()).isEqualTo("MAINTENANCE");
        assertThat(record.getTitle()).isEqualTo("Title");
        assertThat(record.getDescription()).isEqualTo("Description");
        assertThat(record.getPerformedAt()).isEqualTo(performedAt);
        assertThat(record.getOdometerKm()).isEqualTo(1000L);
        assertThat(record.getKind()).isEqualTo("OIL");
        assertThat(record.getMetaJson()).isEqualTo("{\"liters\":25}");
        assertThat(record.getCurrency()).isEqualTo("USD");
        assertThat(record.getTotalCost()).isEqualTo(new BigDecimal("50.00"));
        assertThat(record.getId()).isNull();
    }
}