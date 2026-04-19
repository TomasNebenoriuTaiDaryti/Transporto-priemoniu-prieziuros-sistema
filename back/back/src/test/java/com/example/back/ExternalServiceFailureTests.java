package com.example.back;

import com.example.back.ai.service.GeminiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExternalServiceFailureTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GeminiChatService geminiChatService;

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

    @Test
    @DisplayName("RBR-1: sistema turi stabiliai apdoroti išorinės paslaugos klaidą")
    void systemShouldHandleExternalAiFailureGracefully() throws Exception {
        when(geminiChatService.ask(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenThrow(new RuntimeException("Gemini unavailable"));
        String token = getJwtToken();
        String body = """
            {
              "vehicleId": 8,
              "message": "Kas negerai su stabdžiais?",
              "history": []
            }
            """;

        mockMvc.perform(post("/api/ai/chat").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is5xxServerError());
    }
}