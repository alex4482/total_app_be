package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.email.EmailData;
import com.work.total_app.models.email.EmailFileKeywordPair;
import com.work.total_app.models.email.EmailFileKeywordsDto;
import com.work.total_app.models.email.SendEmailsDto;
import com.work.total_app.services.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/emails")
@CrossOrigin(origins = {AuthenticationConstants.PROD_WEBSITE_URL, }, originPatterns = {AuthenticationConstants.LOCAL_WEBSITE_PATTERN, AuthenticationConstants.STAGING_WEBSITE_PATTERN})
@Log4j2
public class EmailController {

    @Autowired
    private EmailService service;

    @PostMapping
    public List<EmailData> sendEmails(@RequestBody SendEmailsDto dto)
    {
        return service.sendEmails(dto.getData());
    }

    @GetMapping("/invoice-presets")
    public EmailFileKeywordsDto getInvoicePresets()
    {
        List<EmailFileKeywordPair> presets = service.getInvoicePresets();
        EmailFileKeywordsDto dto = new EmailFileKeywordsDto();
        dto.setPresets(presets);
        return dto;
    }

    @PostMapping("/invoice-presets")
    public EmailFileKeywordsDto postInvoicePresets(@RequestBody EmailFileKeywordsDto presetsDto)
    {
        List<EmailFileKeywordPair> savedPresets = service.saveInvoicePresets(presetsDto.getPresets());
        if (savedPresets.size() != presetsDto.getPresets().size())
        {
            // TODO: log error
        }
        EmailFileKeywordsDto dto = new EmailFileKeywordsDto();
        dto.setPresets(savedPresets);
        return dto;
    }
}
