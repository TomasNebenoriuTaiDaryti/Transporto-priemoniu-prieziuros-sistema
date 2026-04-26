package com.example.back.Unit.security;

import com.example.back.security.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    @DisplayName("Sukuria ObjectMapper bean")
    void objectMapperCreatesBean() throws Exception {
        JacksonConfig config = new JacksonConfig();

        ObjectMapper objectMapper = config.objectMapper();

        assertThat(objectMapper).isNotNull();
        assertThat(objectMapper.writeValueAsString(Map.of("name", "test"))).isEqualTo("{\"name\":\"test\"}");
    }
}