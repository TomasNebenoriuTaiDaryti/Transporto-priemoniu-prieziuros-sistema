package com.example.back.Unit.record;

import com.example.back.record.dto.RecordDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RecordDtosTest {

    @Test
    @DisplayName("RecordKind turi visas reiksmes")
    void recordKindHasAllValues() {
        assertThat(Arrays.asList(RecordDtos.RecordKind.values())).containsExactly(RecordDtos.RecordKind.SERVICE, RecordDtos.RecordKind.TIRES, RecordDtos.RecordKind.BRAKES, RecordDtos.RecordKind.OIL, RecordDtos.RecordKind.OTHER, RecordDtos.RecordKind.FUEL);
        assertThat(RecordDtos.RecordKind.valueOf("OIL")).isEqualTo(RecordDtos.RecordKind.OIL);
    }

    @Test
    @DisplayName("RecordResponse issaugo laukus")
    void recordResponseStoresFields() {
        Instant performedAt = Instant.parse("2026-01-01T10:00:00Z");
        RecordDtos.RecordResponse response = new RecordDtos.RecordResponse(5L, 1L, RecordDtos.RecordKind.OIL, "Title", "Desc", performedAt, 1000L, new BigDecimal("50.00"), "EUR", "{}", 10L, true);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.kind()).isEqualTo(RecordDtos.RecordKind.OIL);
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.description()).isEqualTo("Desc");
        assertThat(response.performedAt()).isEqualTo(performedAt);
        assertThat(response.odometerKm()).isEqualTo(1000L);
        assertThat(response.totalCost()).isEqualTo(new BigDecimal("50.00"));
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.metaJson()).isEqualTo("{}");
        assertThat(response.createdByUserId()).isEqualTo(10L);
        assertThat(response.canEdit()).isTrue();
    }

    @Test
    @DisplayName("CreateRecordRequest issaugo laukus")
    void createRecordRequestStoresFields() {
        Instant performedAt = Instant.parse("2026-01-01T10:00:00Z");
        RecordDtos.CreateRecordRequest request = new RecordDtos.CreateRecordRequest(RecordDtos.RecordKind.FUEL, "Fuel", "Desc", performedAt, 1000L, new BigDecimal("50.00"), "EUR", "WINTER", 2, new BigDecimal("25.5"));

        assertThat(request.kind()).isEqualTo(RecordDtos.RecordKind.FUEL);
        assertThat(request.title()).isEqualTo("Fuel");
        assertThat(request.description()).isEqualTo("Desc");
        assertThat(request.performedAt()).isEqualTo(performedAt);
        assertThat(request.odometerKm()).isEqualTo(1000L);
        assertThat(request.totalCost()).isEqualTo(new BigDecimal("50.00"));
        assertThat(request.currency()).isEqualTo("EUR");
        assertThat(request.tireType()).isEqualTo("WINTER");
        assertThat(request.tireAgeYears()).isEqualTo(2);
        assertThat(request.liters()).isEqualTo(new BigDecimal("25.5"));
    }

    @Test
    @DisplayName("UpdateRecordRequest issaugo laukus")
    void updateRecordRequestStoresFields() {
        Instant performedAt = Instant.parse("2026-01-01T10:00:00Z");
        RecordDtos.UpdateRecordRequest request = new RecordDtos.UpdateRecordRequest(RecordDtos.RecordKind.TIRES, "Tires", "Desc", performedAt, 1000L, new BigDecimal("50.00"), "EUR", "SUMMER", 3, new BigDecimal("20.0"));

        assertThat(request.kind()).isEqualTo(RecordDtos.RecordKind.TIRES);
        assertThat(request.title()).isEqualTo("Tires");
        assertThat(request.description()).isEqualTo("Desc");
        assertThat(request.performedAt()).isEqualTo(performedAt);
        assertThat(request.odometerKm()).isEqualTo(1000L);
        assertThat(request.totalCost()).isEqualTo(new BigDecimal("50.00"));
        assertThat(request.currency()).isEqualTo("EUR");
        assertThat(request.tireType()).isEqualTo("SUMMER");
        assertThat(request.tireAgeYears()).isEqualTo(3);
        assertThat(request.liters()).isEqualTo(new BigDecimal("20.0"));
    }

    @Test
    @DisplayName("Privatus konstruktorius gali buti iskviestas refleksija")
    void privateConstructorCanBeCalledByReflection() throws Exception {
        Constructor<RecordDtos> constructor = RecordDtos.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        RecordDtos result = constructor.newInstance();

        assertThat(result).isNotNull();
    }
}