package com.example.back.Unit.vehicle.vin;

import com.example.back.vehicle.vin.VincarioConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VincarioConfigTest {

    @Test
    @DisplayName("Konfigūracija išsaugo Vincario nustatymus")
    void configStoresVincarioSettings() {
        VincarioConfig config = new VincarioConfig("https://api.vindecoder.eu", "api-key", "secret-key", "json");

        assertThat(config.baseUrl()).isEqualTo("https://api.vindecoder.eu");
        assertThat(config.apiKey()).isEqualTo("api-key");
        assertThat(config.secretKey()).isEqualTo("secret-key");
        assertThat(config.format()).isEqualTo("json");
    }
}