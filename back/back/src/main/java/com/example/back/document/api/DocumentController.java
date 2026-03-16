package com.example.back.document.api;


import com.example.back.document.dto.DocumentDtos;
import com.example.back.document.service.DocumentService;
import com.example.back.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentDtos.DocumentResponse> list(@PathVariable Long vehicleId, Authentication auth) {Long userId = AuthUser.userId(auth);
        return service.list(userId, vehicleId);
    }

    @PostMapping("/insurance")
    public DocumentDtos.DocumentResponse addInsurance(@PathVariable Long vehicleId, @Valid @RequestBody DocumentDtos.CreateInsuranceRequest req, Authentication auth) {Long userId = AuthUser.userId(auth);
        return service.addInsurance(userId, vehicleId, req);
    }

    @PostMapping("/inspection")
    public DocumentDtos.DocumentResponse addInspection(@PathVariable Long vehicleId, @Valid @RequestBody DocumentDtos.CreateInspectionRequest req, Authentication auth) {Long userId = AuthUser.userId(auth);
        return service.addInspection(userId, vehicleId, req);
    }

    @PostMapping("/fine")
    public DocumentDtos.DocumentResponse addFine(@PathVariable Long vehicleId, @Valid @RequestBody DocumentDtos.CreateFineRequest req, Authentication auth) {Long userId = AuthUser.userId(auth);
        return service.addFine(userId, vehicleId, req);
    }

    @PostMapping("/other")
    public DocumentDtos.DocumentResponse addOther(@PathVariable Long vehicleId, @Valid @RequestBody DocumentDtos.CreateOtherDocRequest req, Authentication auth) {Long userId = AuthUser.userId(auth);
        return service.addOther(userId, vehicleId, req);
    }

    @PutMapping("/{docId}")
    public DocumentDtos.DocumentResponse update(@PathVariable Long vehicleId, @PathVariable UUID docId, @Valid @RequestBody DocumentDtos.UpdateDocRequest req, Authentication auth) {Long userId = AuthUser.userId(auth);
        return service.update(userId, docId, req);
    }

    @DeleteMapping("/{docId}")
    public void delete(@PathVariable Long vehicleId, @PathVariable UUID docId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        service.delete(userId, docId);
    }
}
