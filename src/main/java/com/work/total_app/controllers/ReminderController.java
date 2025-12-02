package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.reminder.CreateReminderDto;
import com.work.total_app.models.reminder.ReminderDto;
import com.work.total_app.models.reminder.UpdateReminderDto;
import com.work.total_app.services.ReminderService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/reminders")
@Log4j2
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<ReminderDto>> createReminder(@RequestBody CreateReminderDto dto) {
        ReminderDto created = reminderService.createReminder(dto);
        return ResponseEntity.ok(ApiResponse.success("Reminder created successfully", created));
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ReminderDto>>> getAllReminders() {
        List<ReminderDto> reminders = reminderService.getAllReminders();
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<ReminderDto>> getReminderById(@PathVariable UUID id) {
        ReminderDto reminder = reminderService.getReminderById(id);
        return ResponseEntity.ok(ApiResponse.success(reminder));
    }

    @GetMapping("/grouping/{grouping}")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ReminderDto>>> getRemindersByGrouping(@PathVariable String grouping) {
        List<ReminderDto> reminders = reminderService.getRemindersByGrouping(grouping);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteReminder(@PathVariable UUID id) {
        reminderService.deleteReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder deleted successfully", null));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<ReminderDto>> updateReminder(@PathVariable UUID id, @RequestBody UpdateReminderDto dto) {
        ReminderDto updated = reminderService.updateReminder(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Reminder updated successfully", updated));
    }

    @PutMapping("/{id}/active")
    @ResponseBody
    public ResponseEntity<ApiResponse<ReminderDto>> setReminderActive(@PathVariable UUID id, @RequestParam boolean active) {
        ReminderDto reminder = reminderService.setReminderActive(id, active);
        String message = active ? "Reminder activated successfully" : "Reminder stopped successfully";
        return ResponseEntity.ok(ApiResponse.success(message, reminder));
    }
}

