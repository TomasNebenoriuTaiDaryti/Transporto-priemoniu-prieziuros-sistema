package com.example.back;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataIntegrityTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

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
    @DisplayName("PKR-1: sukūrus ir ištrynus dokumentą susiję duomenys turi išlikti nuoseklūs")
    void relatedDataShouldRemainConsistentAfterDocumentCreateAndDelete() throws Exception {
        String token = getJwtToken();
        long vehicleId = getAccessibleVehicleId(token);
        Integer remindersBefore = jdbc.queryForObject(
                "select count(*) from reminder where vehicle_id = ?",
                Integer.class,
                vehicleId
        );
        String body = """
            {
              "policyNumber": "TEST-123",
              "startDate": "2026-04-01",
              "endDate": "2027-04-01",
              "price": 120.0
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/vehicles/{vehicleId}/documents/insurance", vehicleId).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        UUID docId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );
        Integer remindersAfterCreate = jdbc.queryForObject(
                "select count(*) from reminder where vehicle_id = ?",
                Integer.class,
                vehicleId
        );

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/documents/{docId}", vehicleId, docId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        Integer remindersAfterDelete = jdbc.queryForObject(
                "select count(*) from reminder where vehicle_id = ?",
                Integer.class,
                vehicleId
        );

        assertTrue(remindersAfterCreate >= remindersBefore);
        assertEquals(remindersBefore, remindersAfterDelete);
    }
}