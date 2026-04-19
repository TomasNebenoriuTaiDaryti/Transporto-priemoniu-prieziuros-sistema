package com.example.back;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatisticsAccuracyTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String getJwtToken() throws Exception {
        String body = """
            {
              "email": "test1@test.com",
              "password": "tomas123"
            }
            """;
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long getAccessibleVehicleId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/vehicles/accessible").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn();
        JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(array.isArray(), "Automobilių sąrašas turi būti masyvas");
        assertTrue(array.size() > 0, "Testo vartotojas turi turėti bent vieną prieinamą automobilį");

        return array.get(0).get("id").asLong();
    }

    @Test
    @DisplayName("VKR-1: statistikos atsakyme turi būti pateikiamos apskaičiuotos reikšmės")
    void statisticsResponseShouldContainCalculatedValues() throws Exception {
        String token = getJwtToken();
        long vehicleId = getAccessibleVehicleId(token);

        mockMvc.perform(get("/api/stats/vehicles/{vehicleId}", vehicleId).header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$.fuelHistory").exists()).andExpect(jsonPath("$.monthlyCosts").exists());
    }
}