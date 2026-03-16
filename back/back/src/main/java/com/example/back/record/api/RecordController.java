package com.example.back.record.api;

import com.example.back.record.dto.RecordDtos;
import com.example.back.record.service.RecordService;
import com.example.back.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/vehicles/{vehicleId}/records")
public class RecordController {

    private final RecordService service;

    public RecordController(RecordService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecordDtos.RecordResponse> list(@PathVariable Long vehicleId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.list(userId, vehicleId);
    }

    @PostMapping
    public RecordDtos.RecordResponse create(@PathVariable Long vehicleId, @Valid @RequestBody RecordDtos.CreateRecordRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.create(userId, vehicleId, req);
    }

    @PutMapping("/{recordId}")
    public RecordDtos.RecordResponse update(@PathVariable Long recordId, @Valid @RequestBody RecordDtos.UpdateRecordRequest req, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        return service.update(userId, recordId, req);
    }

    @DeleteMapping("/{recordId}")
    public void delete(@PathVariable Long recordId, Authentication auth) {
        Long userId = AuthUser.userId(auth);
        service.delete(userId, recordId);
    }
}
