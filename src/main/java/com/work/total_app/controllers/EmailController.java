package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.email.EmailPreset;
import com.work.total_app.models.email.EmailPresetsDto;
import com.work.total_app.models.email.SendEmailsDto;
import com.work.total_app.services.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/email-presets")
@Log4j2
public class EmailController {

    @Autowired
    private EmailService service;

    @PostMapping("/send-emails")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<EmailData>>> sendEmails(@RequestBody SendEmailsDto dto)
    {
        List<EmailData> failedEmails = service.sendEmails(dto.getData());
        
        if (failedEmails.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("All emails sent successfully", null));
        } else {
            return ResponseEntity.ok(ApiResponse.error(
                String.format("Failed to send %d out of %d emails", failedEmails.size(), dto.getData().size()),
                failedEmails
            ));
        }
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<EmailPresetsDto>> getInvoicePresets()
    {
        List<EmailPreset> presets = service.getInvoicePresets();
        EmailPresetsDto dto = new EmailPresetsDto();
        dto.setPresets(presets);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/bulk")
    @ResponseBody
    public ResponseEntity<ApiResponse<EmailPresetsDto>> postInvoicePresets(@RequestBody EmailPresetsDto presetsDto)
    {
        List<EmailPreset> savedPresets = service.saveInvoicePresets(presetsDto.getPresets());
        
        if (savedPresets.size() != presetsDto.getPresets().size()) {
            log.error("Failed to save all presets. Expected: {}, Saved: {}", presetsDto.getPresets().size(), savedPresets.size());
            return ResponseEntity.status(500).body(ApiResponse.error(
                "Failed to save all presets",
                new EmailPresetsDto()
            ));
        }
        
        EmailPresetsDto dto = new EmailPresetsDto();
        dto.setPresets(savedPresets);
        return ResponseEntity.ok(ApiResponse.success("Presets saved successfully", dto));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<EmailPreset>> postSingleInvoicePreset(@RequestBody EmailPreset preset)
    {
        EmailPreset saved = service.saveSingleInvoicePreset(preset);
        return ResponseEntity.ok(ApiResponse.success("Preset saved successfully", saved));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<EmailPreset>> updateInvoicePreset(@PathVariable Integer id, @RequestBody EmailPreset preset)
    {
        EmailPreset updated = service.updateInvoicePreset(id, preset);
        return ResponseEntity.ok(ApiResponse.success("Preset updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteInvoicePreset(@PathVariable Integer id)
    {
        service.deleteInvoicePreset(id);
        return ResponseEntity.ok(ApiResponse.success("Preset deleted successfully", null));
    }
}
