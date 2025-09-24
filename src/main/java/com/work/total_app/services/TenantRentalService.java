package com.work.total_app.services;

import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.TenantRentalData;
import com.work.total_app.models.tenant.TenantRentalDto;
import com.work.total_app.repositories.RentalSpaceRepository;
import com.work.total_app.repositories.TenantRentalDataRepository;
import com.work.total_app.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class TenantRentalService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RentalSpaceRepository rentalSpaceRepository;

    @Autowired
    private TenantRentalDataRepository tenantRentalRepository;

    public TenantRentalData startNewRent(TenantRentalDto dto) {
        TenantRentalData trd = new TenantRentalData();

        if (!tenantRepository.existsById(dto.tenantId()))
        {
            throw new ValidationException("Invalid tenant id for new rental start.");
        }
        // check tennant exists
        trd.setTenantId(dto.tenantId());

        if (!rentalSpaceRepository.existsById(dto.rentalSpaceId()))
        {
            throw new ValidationException("Invalid rental space id for new rental start.");
        }
        // check rental space exists
        trd.setRentalSpaceId(dto.rentalSpaceId());

        trd.setStartDate(dto.startDate());
        trd.addPriceChange(dto.price(), Date.from(Instant.now()));

        return tenantRentalRepository.save(trd);
    }
}
