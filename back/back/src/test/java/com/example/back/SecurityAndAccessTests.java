package com.example.back;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndAccessTests {
    @Autowired
    MockMvc mockMvc;

    private String getJwtToken() throws Exception {
        String body = """
            {
              "email": "test1@test.com",
              "password": "tomas123"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        return new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("SGR-1 ir AGR-1: be JWT apsaugotas endpointas turi būti nepasiekiamas")
    void protectedEndpointShouldBeUnavailableWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/vehicles/accessible")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SGR-1: su galiojančiu JWT apsaugotas endpointas turi būti pasiekiamas")
    void protectedEndpointShouldBeAvailableWithJwt() throws Exception {
        String token = getJwtToken();
        mockMvc.perform(get("/api/vehicles/accessible").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("AGR-2: vartotojas neturi pasiekti jam neleidžiamų duomenų")
    void userShouldNotAccessUnauthorizedData() throws Exception {
        String token = getJwtToken();
        mockMvc.perform(get("/api/vehicles/999999/documents").header("Authorization", "Bearer " + token)).andExpect(status().is4xxClientError());
    }
}