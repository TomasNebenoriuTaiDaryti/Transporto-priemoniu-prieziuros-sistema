package com.example.back.reminder.api;

import com.example.back.reminder.dto.ReminderDtos;
import com.example.back.reminder.service.ReminderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/reminders")
public class ReminderController {

    private final ReminderService reminders;

    public ReminderController(ReminderService reminders) {
        this.reminders = reminders;
    }

    @GetMapping
    public List<ReminderDtos.ReminderResponse> list(@PathVariable Long vehicleId, Authentication auth) {
        return reminders.list(vehicleId, auth);
    }
}