package com.example.back.Unit.ai;

import com.example.back.ai.dto.AiChatRequest;
import com.example.back.ai.dto.AiChatResponse;
import com.example.back.ai.dto.AiImageAnalysisResponse;
import com.example.back.ai.dto.HistoryMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiDtosTest {

    @Test
    @DisplayName("AiChatRequest issaugo laukus")
    void aiChatRequestStoresFields() {
        HistoryMessage history = new HistoryMessage("user", "Sveiki");
        AiChatRequest request = new AiChatRequest(1L, "Klausimas", List.of(history));

        assertThat(request.vehicleId()).isEqualTo(1L);
        assertThat(request.message()).isEqualTo("Klausimas");
        assertThat(request.history()).containsExactly(history);
    }

    @Test
    @DisplayName("AiChatResponse issaugo atsakyma")
    void aiChatResponseStoresAnswer() {
        AiChatResponse response = new AiChatResponse("Atsakymas");

        assertThat(response.answer()).isEqualTo("Atsakymas");
    }

    @Test
    @DisplayName("AiImageAnalysisResponse issaugo atsakyma")
    void aiImageAnalysisResponseStoresAnswer() {
        AiImageAnalysisResponse response = new AiImageAnalysisResponse("Analize");

        assertThat(response.answer()).isEqualTo("Analize");
    }

    @Test
    @DisplayName("HistoryMessage issaugo role ir teksta")
    void historyMessageStoresRoleAndText() {
        HistoryMessage message = new HistoryMessage("assistant", "Labas");

        assertThat(message.role()).isEqualTo("assistant");
        assertThat(message.text()).isEqualTo("Labas");
    }
}