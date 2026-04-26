package com.example.back.Unit.vehicle;

import com.example.back.vehicle.service.FuelTypeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FuelTypeMapperTest {

    @Test
    @DisplayName("Kuro tipai normalizuojami į duomenų bazės kodus")
    void normalizeFuelTypesToDbCodes() {
        assertThat(FuelTypeMapper.normalizeToDbCode(null)).isNull();
        assertThat(FuelTypeMapper.normalizeToDbCode(" diesel ")).isEqualTo("DIESEL");
        assertThat(FuelTypeMapper.normalizeToDbCode("gasoline")).isEqualTo("PETROL");
        assertThat(FuelTypeMapper.normalizeToDbCode("petrol")).isEqualTo("PETROL");
        assertThat(FuelTypeMapper.normalizeToDbCode("electric")).isEqualTo("EV");
        assertThat(FuelTypeMapper.normalizeToDbCode("lpg")).isEqualTo("LPG");
        assertThat(FuelTypeMapper.normalizeToDbCode("cng")).isEqualTo("CNG");
        assertThat(FuelTypeMapper.normalizeToDbCode("plug-in hybrid")).isEqualTo("PHEV");
        assertThat(FuelTypeMapper.normalizeToDbCode("phev")).isEqualTo("PHEV");
        assertThat(FuelTypeMapper.normalizeToDbCode("hybrid")).isEqualTo("HYBRID");
        assertThat(FuelTypeMapper.normalizeToDbCode("hydrogen")).isEqualTo("OTHER");
    }
}