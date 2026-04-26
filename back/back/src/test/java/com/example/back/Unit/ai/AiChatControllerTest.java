package com.example.back.Unit.ai;

import com.example.back.ai.api.AiChatController;
import com.example.back.ai.dto.AiChatRequest;
import com.example.back.ai.dto.AiChatResponse;
import com.example.back.ai.dto.AiImageAnalysisResponse;
import com.example.back.ai.dto.HistoryMessage;
import com.example.back.ai.service.GeminiChatService;
import com.example.back.security.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatControllerTest {

    private final GeminiChatService geminiChatService = mock(GeminiChatService.class);
    private final AiChatController controller = new AiChatController(geminiChatService);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Chat perduoda duomenis servisui")
    void chatDelegatesToService() {
        AiChatRequest request = new AiChatRequest(1L, "Kas blogai?", List.of(new HistoryMessage("user", "Sveiki")));
        AiChatResponse response = new AiChatResponse("Atsakymas");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(geminiChatService.ask(10L, 1L, "Kas blogai?", request.history())).thenReturn(response);

            AiChatResponse result = controller.chat(request, auth);

            assertThat(result).isEqualTo(response);
            verify(geminiChatService).ask(10L, 1L, "Kas blogai?", request.history());
        }
    }

    @Test
    @DisplayName("Dashboard image perduoda failo bytes ir mime type servisui")
    void analyzeDashboardImageDelegatesToService() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "dash.png", "image/png", new byte[]{1, 2, 3});
        AiImageAnalysisResponse response = new AiImageAnalysisResponse("Analize");

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(geminiChatService.analyzeDashboardImage(10L, 1L, new byte[]{1, 2, 3}, "image/png")).thenReturn(response);

            AiImageAnalysisResponse result = controller.analyzeDashboardImage(1L, image, auth);

            assertThat(result).isEqualTo(response);
            verify(geminiChatService).analyzeDashboardImage(10L, 1L, new byte[]{1, 2, 3}, "image/png");
        }
    }

    @Test
    @DisplayName("Tuscias dashboard image meta klaida")
    void analyzeDashboardImageThrowsWhenFileIsEmpty() {
        MockMultipartFile image = new MockMultipartFile("image", "dash.png", "image/png", new byte[]{});

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            assertThatThrownBy(() -> controller.analyzeDashboardImage(1L, image, auth)).isInstanceOf(IllegalArgumentException.class).hasMessage("Nuotrauka neįkelta");

            verifyNoInteractions(geminiChatService);
        }
    }

    @Test
    @DisplayName("Netinkamas mime type meta klaida")
    void analyzeDashboardImageThrowsWhenMimeTypeIsInvalid() {
        MockMultipartFile image = new MockMultipartFile("image", "dash.gif", "image/gif", new byte[]{1});

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            assertThatThrownBy(() -> controller.analyzeDashboardImage(1L, image, auth)).isInstanceOf(IllegalArgumentException.class).hasMessage("Leidžiamos tik JPG, PNG arba WEBP nuotraukos");

            verifyNoInteractions(geminiChatService);
        }
    }

    @Test
    @DisplayName("Null mime type meta klaida")
    void analyzeDashboardImageThrowsWhenMimeTypeIsNull() {
        MockMultipartFile image = new MockMultipartFile("image", "dash", null, new byte[]{1});

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            assertThatThrownBy(() -> controller.analyzeDashboardImage(1L, image, auth)).isInstanceOf(IllegalArgumentException.class).hasMessage("Leidžiamos tik JPG, PNG arba WEBP nuotraukos");

            verifyNoInteractions(geminiChatService);
        }
    }

    @Test
    @DisplayName("Leidzia jpeg ir webp mime tipus")
    void analyzeDashboardImageAllowsJpegAndWebp() throws Exception {
        MockMultipartFile jpeg = new MockMultipartFile("image", "dash.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile webp = new MockMultipartFile("image", "dash.webp", "image/webp", new byte[]{2});

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(geminiChatService.analyzeDashboardImage(eq(10L), eq(1L), any(byte[].class), anyString())).thenReturn(new AiImageAnalysisResponse("ok"));

            assertThat(controller.analyzeDashboardImage(1L, jpeg, auth).answer()).isEqualTo("ok");
            assertThat(controller.analyzeDashboardImage(1L, webp, auth).answer()).isEqualTo("ok");

            verify(geminiChatService).analyzeDashboardImage(10L, 1L, new byte[]{1}, "image/jpeg");
            verify(geminiChatService).analyzeDashboardImage(10L, 1L, new byte[]{2}, "image/webp");
        }
    }
}