package com.example.back;

import com.example.back.auth.repo.AccountRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndAccessTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountRepo accountRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

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

    @Test
    @Transactional
    @DisplayName("SGR-3: vartotojo slaptažodis duomenų bazėje turi būti saugomas maišos forma")
    void passwordShouldBeStoredAsHash() throws Exception {
        String email = "sgrtest@test.com";
        String password = "tomas123";
        String body = """
        {
          "email": "%s",
          "password": "%s"
        }
        """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());

        var account = accountRepo.findByEmail(email).orElseThrow();

        assertNotEquals(password, account.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, account.getPasswordHash()));
    }
}