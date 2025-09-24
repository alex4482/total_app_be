package com.work.total_app.services;

import com.work.total_app.models.tenant.Tenant;
import com.work.total_app.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class TenantService {

    @Autowired
    private TenantRepository TenantRepository;

    public List<Tenant> getTenants(Boolean isActive) {

        if (isActive != null)
        {
            return TenantRepository.findByActive(isActive);
        }
        return TenantRepository.findAll();
    }

    public Tenant addTenant(Tenant tenant) {
        tenant.setId(tenant.getName().toLowerCase(Locale.ROOT).replace(' ', '-'));
        Tenant t = TenantRepository.save(tenant);
        return t;
    }

    public void deleteTenant(String id) {
        TenantRepository.deleteById(id);
    }

    public Tenant getTenant(String id) {
        return TenantRepository.findById(id).orElse(null);
    }
}
