package com.example.back.Unit.record;

import com.example.back.record.api.RecordController;
import com.example.back.record.dto.RecordDtos;
import com.example.back.record.service.RecordService;
import com.example.back.security.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecordControllerTest {

    private final RecordService service = mock(RecordService.class);
    private final RecordController controller = new RecordController(service);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Sarasas grazinamas pagal prisijungusi vartotoja")
    void listDelegatesToService() {
        List<RecordDtos.RecordResponse> response = List.of(response());

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.list(10L, 1L)).thenReturn(response);

            List<RecordDtos.RecordResponse> result = controller.list(1L, auth);

            assertThat(result).isEqualTo(response);
            verify(service).list(10L, 1L);
        }
    }

    @Test
    @DisplayName("Sukurimas perduoda duomenis servisui")
    void createDelegatesToService() {
        RecordDtos.CreateRecordRequest request = createRequest(RecordDtos.RecordKind.OIL);
        RecordDtos.RecordResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.create(10L, 1L, request)).thenReturn(response);

            RecordDtos.RecordResponse result = controller.create(1L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).create(10L, 1L, request);
        }
    }

    @Test
    @DisplayName("Atnaujinimas perduoda duomenis servisui")
    void updateDelegatesToService() {
        RecordDtos.UpdateRecordRequest request = updateRequest(RecordDtos.RecordKind.SERVICE);
        RecordDtos.RecordResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.update(10L, 5L, request)).thenReturn(response);

            RecordDtos.RecordResponse result = controller.update(5L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).update(10L, 5L, request);
        }
    }

    @Test
    @DisplayName("Trynimas iskviecia servisa")
    void deleteDelegatesToService() {
        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.delete(5L, auth);

            verify(service).delete(10L, 5L);
        }
    }

    private static RecordDtos.RecordResponse response() {
        return new RecordDtos.RecordResponse(5L, 1L, RecordDtos.RecordKind.OIL, "Tepalu ir filtro keitimas", "desc", Instant.parse("2026-01-01T10:00:00Z"), 1000L, new BigDecimal("50.00"), "EUR", "{}", 10L, true);
    }

    private static RecordDtos.CreateRecordRequest createRequest(RecordDtos.RecordKind kind) {
        return new RecordDtos.CreateRecordRequest(kind, "Title", "desc", Instant.parse("2026-01-01T10:00:00Z"), 1000L, new BigDecimal("50.00"), "EUR", "WINTER", 2, new BigDecimal("25.5"));
    }

    private static RecordDtos.UpdateRecordRequest updateRequest(RecordDtos.RecordKind kind) {
        return new RecordDtos.UpdateRecordRequest(kind, "Title", "desc", Instant.parse("2026-01-01T10:00:00Z"), 1000L, new BigDecimal("50.00"), "EUR", "WINTER", 2, new BigDecimal("25.5"));
    }
}