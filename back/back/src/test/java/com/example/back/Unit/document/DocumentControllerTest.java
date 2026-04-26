package com.example.back.Unit.document;

import com.example.back.document.api.DocumentController;
import com.example.back.document.dto.DocumentDtos;
import com.example.back.document.service.DocumentService;
import com.example.back.security.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DocumentControllerTest {

    private final DocumentService service = mock(DocumentService.class);
    private final DocumentController controller = new DocumentController(service);
    private final Authentication auth = mock(Authentication.class);

    @Test
    @DisplayName("Sarasas perduodamas servisui")
    void listDelegatesToService() {
        List<DocumentDtos.DocumentResponse> response = List.of(response());

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.list(10L, 1L)).thenReturn(response);

            List<DocumentDtos.DocumentResponse> result = controller.list(1L, auth);

            assertThat(result).isEqualTo(response);
            verify(service).list(10L, 1L);
        }
    }

    @Test
    @DisplayName("Draudimo pridejimas perduodamas servisui")
    void addInsuranceDelegatesToService() {
        DocumentDtos.CreateInsuranceRequest request = new DocumentDtos.CreateInsuranceRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 100.0);
        DocumentDtos.DocumentResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.addInsurance(10L, 1L, request)).thenReturn(response);

            DocumentDtos.DocumentResponse result = controller.addInsurance(1L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).addInsurance(10L, 1L, request);
        }
    }

    @Test
    @DisplayName("Technines pridejimas perduodamas servisui")
    void addInspectionDelegatesToService() {
        DocumentDtos.CreateInspectionRequest request = new DocumentDtos.CreateInspectionRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
        DocumentDtos.DocumentResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.addInspection(10L, 1L, request)).thenReturn(response);

            DocumentDtos.DocumentResponse result = controller.addInspection(1L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).addInspection(10L, 1L, request);
        }
    }

    @Test
    @DisplayName("Baudos pridejimas perduodamas servisui")
    void addFineDelegatesToService() {
        DocumentDtos.CreateFineRequest request = new DocumentDtos.CreateFineRequest(LocalDate.of(2026, 1, 1), 50.0, "Speed");
        DocumentDtos.DocumentResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.addFine(10L, 1L, request)).thenReturn(response);

            DocumentDtos.DocumentResponse result = controller.addFine(1L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).addFine(10L, 1L, request);
        }
    }

    @Test
    @DisplayName("Kito dokumento pridejimas perduodamas servisui")
    void addOtherDelegatesToService() {
        DocumentDtos.CreateOtherDocRequest request = new DocumentDtos.CreateOtherDocRequest("Other", "Description", LocalDate.of(2027, 1, 1));
        DocumentDtos.DocumentResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.addOther(10L, 1L, request)).thenReturn(response);

            DocumentDtos.DocumentResponse result = controller.addOther(1L, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).addOther(10L, 1L, request);
        }
    }

    @Test
    @DisplayName("Atnaujinimas perduodamas servisui")
    void updateDelegatesToService() {
        UUID docId = UUID.randomUUID();
        DocumentDtos.UpdateDocRequest request = new DocumentDtos.UpdateDocRequest("Title", "Desc", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}");
        DocumentDtos.DocumentResponse response = response();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);
            when(service.update(10L, docId, request)).thenReturn(response);

            DocumentDtos.DocumentResponse result = controller.update(1L, docId, request, auth);

            assertThat(result).isEqualTo(response);
            verify(service).update(10L, docId, request);
        }
    }

    @Test
    @DisplayName("Trynimas perduodamas servisui")
    void deleteDelegatesToService() {
        UUID docId = UUID.randomUUID();

        try (MockedStatic<AuthUser> authUser = mockStatic(AuthUser.class)) {
            authUser.when(() -> AuthUser.userId(auth)).thenReturn(10L);

            controller.delete(1L, docId, auth);

            verify(service).delete(10L, docId);
        }
    }

    private static DocumentDtos.DocumentResponse response() {
        return new DocumentDtos.DocumentResponse(UUID.randomUUID(), 1L, DocumentDtos.DocumentType.INSURANCE, "Draudimas", null, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "{}", 10L, true);
    }
}