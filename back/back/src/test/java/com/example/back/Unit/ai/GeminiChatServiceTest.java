package com.example.back.Unit.ai;

import com.example.back.ai.dto.AiChatResponse;
import com.example.back.ai.dto.AiImageAnalysisResponse;
import com.example.back.ai.dto.HistoryMessage;
import com.example.back.ai.service.GeminiChatService;
import com.example.back.ai.service.VehicleAiContextService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeminiChatServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Ask kviecia Gemini ir grazina isvalyta atsakyma")
    void askCallsGeminiAndReturnsSanitizedAnswer() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        String baseUrl = startServer(requestedPath, requestedQuery, requestBody, """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "**Atsakymas**" }
                        ]
                      }
                    }
                  ]
                }
                """);

        VehicleAiContextService contextService = mock(VehicleAiContextService.class);
        when(contextService.buildSanitizedContext(10L, 1L)).thenReturn("Auto context");

        GeminiChatService service = service(contextService, baseUrl);

        AiChatResponse result = service.ask(10L, 1L, "Kas blogai?", List.of(new HistoryMessage("assistant", "Senas atsakymas"), new HistoryMessage("something", "Senas klausimas")));

        assertThat(result.answer()).isEqualTo("Atsakymas");
        assertThat(requestedPath.get()).isEqualTo("/v1beta/models/gemini-test:generateContent");
        assertThat(requestedQuery.get()).isEqualTo("key=api-key");
        assertThat(requestBody.get()).contains("Auto context");
        assertThat(requestBody.get()).contains("ANKSTESNIS POKALBIS");
        assertThat(requestBody.get()).contains("assistant: Senas atsakymas");
        assertThat(requestBody.get()).contains("user: Senas klausimas");
        assertThat(requestBody.get()).contains("NAUJAS VARTOTOJO KLAUSIMAS");
        verify(contextService).buildSanitizedContext(10L, 1L);
    }

    @Test
    @DisplayName("Ask grazina fallback kai atsakymas tuscias")
    void askReturnsFallbackWhenAnswerIsBlank() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        String baseUrl = startServer(requestedPath, requestedQuery, requestBody, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \"}]}}]}");

        VehicleAiContextService contextService = mock(VehicleAiContextService.class);
        when(contextService.buildSanitizedContext(10L, 1L)).thenReturn("Auto context");

        GeminiChatService service = service(contextService, baseUrl);

        AiChatResponse result = service.ask(10L, 1L, "Klausimas", null);

        assertThat(result.answer()).isEqualTo("Nepavyko gauti atsakymo. Bandyk paklausti kitaip.");
        assertThat(requestBody.get()).doesNotContain("ANKSTESNIS POKALBIS");
    }

    @Test
    @DisplayName("Ask grazina fallback kai response neturi candidates")
    void askReturnsFallbackWhenResponseHasNoCandidates() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        String baseUrl = startServer(requestedPath, requestedQuery, requestBody, "{}");

        VehicleAiContextService contextService = mock(VehicleAiContextService.class);
        when(contextService.buildSanitizedContext(10L, 1L)).thenReturn("Auto context");

        GeminiChatService service = service(contextService, baseUrl);

        AiChatResponse result = service.ask(10L, 1L, "Klausimas", List.of());

        assertThat(result.answer()).isEqualTo("Nepavyko gauti atsakymo. Bandyk paklausti kitaip.");
    }

    @Test
    @DisplayName("Image analysis siuncia base64 ir grazina atsakyma")
    void analyzeDashboardImageSendsBase64AndReturnsAnswer() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        String baseUrl = startServer(requestedPath, requestedQuery, requestBody, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"# Lempute **matoma**\"}]}}]}");

        VehicleAiContextService contextService = mock(VehicleAiContextService.class);
        when(contextService.buildSanitizedContext(10L, 1L)).thenReturn("Auto context");

        GeminiChatService service = service(contextService, baseUrl);

        AiImageAnalysisResponse result = service.analyzeDashboardImage(10L, 1L, new byte[]{1, 2, 3}, "image/png");

        assertThat(result.answer()).isEqualTo("Lempute matoma");
        assertThat(requestedPath.get()).isEqualTo("/v1beta/models/gemini-test:generateContent");
        assertThat(requestedQuery.get()).isEqualTo("key=api-key");
        assertThat(requestBody.get()).contains("\"mime_type\":\"image/png\"");
        assertThat(requestBody.get()).contains("\"data\":\"AQID\"");
        assertThat(requestBody.get()).contains("Auto context");
    }

    @Test
    @DisplayName("Image analysis grazina fallback kai atsakymo nera")
    void analyzeDashboardImageReturnsFallbackWhenAnswerMissing() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestedQuery = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        String baseUrl = startServer(requestedPath, requestedQuery, requestBody, "{\"candidates\":[]}");

        VehicleAiContextService contextService = mock(VehicleAiContextService.class);
        when(contextService.buildSanitizedContext(10L, 1L)).thenReturn("Auto context");

        GeminiChatService service = service(contextService, baseUrl);

        AiImageAnalysisResponse result = service.analyzeDashboardImage(10L, 1L, new byte[]{1}, "image/jpeg");

        assertThat(result.answer()).isEqualTo("Nepavyko atpažinti nuotraukos. Pabandyk įkelti ryškesnę skydelio nuotrauką.");
    }

    private String startServer(AtomicReference<String> requestedPath, AtomicReference<String> requestedQuery, AtomicReference<String> requestBody, String responseJson) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            requestedQuery.set(exchange.getRequestURI().getQuery());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static GeminiChatService service(VehicleAiContextService contextService, String baseUrl) {
        GeminiChatService service = new GeminiChatService(contextService, baseUrl);
        setField(service, "apiKey", "api-key");
        setField(service, "model", "gemini-test");
        return service;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = GeminiChatService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}