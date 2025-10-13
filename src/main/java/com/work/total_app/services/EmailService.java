package com.work.total_app.services;

import com.work.total_app.helpers.EmailHelper;
import com.work.total_app.models.email.*;
import com.work.total_app.repositories.EmailFileKeywordPairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private EmailHelper helper;

    @Autowired
    private EmailFileKeywordPairRepository repository;

    public List<EmailPreset> getInvoicePresets() {
        return repository.findAll();
    }

    public List<EmailPreset> saveInvoicePresets(List<EmailPreset> presets) {
        repository.deleteAll();
        return repository.saveAll(presets);
    }

    public List<EmailData> sendEmails(List<EmailData> data) {
        List<EmailData> notSent = new ArrayList<>();
        for (EmailData e : data)
        {
            EEmailSendStatus status = helper.createAndSendMail(e);
            if (status != EEmailSendStatus.OK)
            {
                notSent.add(e);
            }
        }
        return notSent;
    }
}
