package com.example.back.ai.api;

import com.example.back.ai.dto.AiChatRequest;
import com.example.back.ai.dto.AiChatResponse;
import com.example.back.ai.dto.AiImageAnalysisResponse;
import com.example.back.ai.service.GeminiChatService;
import com.example.back.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final GeminiChatService geminiChatService;

    public AiChatController(GeminiChatService geminiChatService) {
        this.geminiChatService = geminiChatService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return geminiChatService.ask(userId, request.vehicleId(), request.message(), request.history());
    }

    @PostMapping(value = "/dashboard-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiImageAnalysisResponse analyzeDashboardImage(@RequestParam("vehicleId") Long vehicleId, @RequestPart("image") MultipartFile image, Authentication auth) throws Exception {
        Long userId = AuthUser.userId(auth);

        if (image.isEmpty()) {
            throw new IllegalArgumentException("Nuotrauka neįkelta");
        }

        String mimeType = image.getContentType();
        if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp"))) {
            throw new IllegalArgumentException("Leidžiamos tik JPG, PNG arba WEBP nuotraukos");
        }

        return geminiChatService.analyzeDashboardImage(userId, vehicleId, image.getBytes(), mimeType);
    }
}
