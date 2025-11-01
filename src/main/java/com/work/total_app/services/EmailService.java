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
        // Nu mai ștergem toate - salvăm fiecare preset individual
        // Dacă există ID, face UPDATE automat
        // Dacă nu există ID dar există după nume, face UPDATE
        // Dacă nu există deloc, face INSERT
        return repository.saveAll(presets.stream()
                .map(preset -> {
                    // Dacă nu are ID, verificăm dacă există după nume
                    if (preset.getId() == null && preset.getName() != null) {
                        repository.findByName(preset.getName()).ifPresent(existing -> {
                            preset.setId(existing.getId());
                        });
                    }
                    return preset;
                })
                .toList());
    }

    public EmailPreset saveSingleInvoicePreset(EmailPreset preset) {
        // Verifică dacă există deja un preset cu același nume
        if (preset.getName() != null && !preset.getName().trim().isEmpty()) {
            repository.findByName(preset.getName()).ifPresent(existing -> {
                // Păstrează ID-ul existent pentru a face update
                preset.setId(existing.getId());
            });
        }
        return repository.save(preset);
    }

    public void deleteInvoicePreset(Integer id) {
        repository.deleteById(id);
    }

    public List<EmailData> sendEmails(List<EmailData> data) {
        List<EmailData> notSent = new ArrayList<>();
        for (EmailData e : data)
        {
            // Validare: verifică dacă există destinatari
            if (e.getRecipients() == null || e.getRecipients().length == 0)
            {
                e.setErrorMessage("Nu există destinatari specificați");
                notSent.add(e);
                continue;
            }

            // Validare: verifică formatul emailurilor
            String invalidEmail = validateEmailAddresses(e.getRecipients());
            if (invalidEmail != null)
            {
                e.setErrorMessage("Email invalid: " + invalidEmail);
                notSent.add(e);
                continue;
            }

            // Validare: verifică subject
            if (e.getSubject() == null || e.getSubject().trim().isEmpty())
            {
                e.setErrorMessage("Subject-ul este obligatoriu");
                notSent.add(e);
                continue;
            }

            // Validare: verifică message
            if (e.getMessage() == null || e.getMessage().trim().isEmpty())
            {
                e.setErrorMessage("Mesajul este obligatoriu");
                notSent.add(e);
                continue;
            }

            // Încearcă să trimită emailul
            EEmailSendStatus status = helper.createAndSendMail(e);
            if (status != EEmailSendStatus.OK)
            {
                e.setErrorMessage("Eroare la trimiterea emailului");
                notSent.add(e);
            }
        }
        return notSent;
    }

    private String validateEmailAddresses(String[] emails) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        for (String email : emails)
        {
            if (email == null || email.trim().isEmpty())
            {
                return "(email gol)";
            }
            if (!email.matches(emailRegex))
            {
                return email;
            }
        }
        return null;
    }
}
