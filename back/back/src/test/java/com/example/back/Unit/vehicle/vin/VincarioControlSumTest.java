package com.example.back.Unit.vehicle.vin;

import com.example.back.vehicle.vin.VincarioControlSum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VincarioControlSumTest {

    @Test
    @DisplayName("Kontrolinė suma sugeneruojama iš VIN, operacijos, API rakto ir slapto rakto")
    void controlSumIsGeneratedFromVinOperationApiKeyAndSecretKey() {
        String result = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000001", "decode", "api-key", "secret-key");

        assertThat(result).isEqualTo("20f59b873e");
    }

    @Test
    @DisplayName("Kontrolinė suma visada turi dešimt simbolių")
    void controlSumHasTenCharacters() {
        String result = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000001", "decode", "api-key", "secret-key");

        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("Kontrolinė suma yra deterministinė")
    void controlSumIsDeterministic() {
        String first = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000001", "decode", "api-key", "secret-key");
        String second = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000001", "decode", "api-key", "secret-key");

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("Skirtingi duomenys sugeneruoja skirtingą kontrolinę sumą")
    void differentInputGeneratesDifferentControlSum() {
        String first = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000001", "decode", "api-key", "secret-key");
        String second = VincarioControlSum.forVinDecode("WVWZZZ1JZXW000002", "decode", "api-key", "secret-key");

        assertThat(second).isNotEqualTo(first);
    }
}