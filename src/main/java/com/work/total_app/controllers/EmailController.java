package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.email.EmailPreset;
import com.work.total_app.models.email.EmailPresetsDto;
import com.work.total_app.models.email.SendEmailsDto;
import com.work.total_app.services.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<EmailData> sendEmails(@RequestBody SendEmailsDto dto)
    {
        return service.sendEmails(dto.getData());
    }

    @GetMapping
    public EmailPresetsDto getInvoicePresets()
    {
        List<EmailPreset> presets = service.getInvoicePresets();
        EmailPresetsDto dto = new EmailPresetsDto();
        dto.setPresets(presets);
        return dto;
    }

    @PostMapping
    public EmailPresetsDto postInvoicePresets(@RequestBody EmailPresetsDto presetsDto)
    {
        List<EmailPreset> savedPresets = service.saveInvoicePresets(presetsDto.getPresets());
        if (savedPresets.size() != presetsDto.getPresets().size())
        {
            // TODO: log error
        }
        EmailPresetsDto dto = new EmailPresetsDto();
        dto.setPresets(savedPresets);
        return dto;
    }
}
