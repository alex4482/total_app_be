package com.work.total_app.services;

import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.Tenant;
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
        if (dto.tenantId() == null || dto.rentalSpaceId() == null)
            throw new ValidationException("Missing id for new rent start: " + dto);

        Tenant t = tenantRepository.findById(dto.tenantId()).orElseThrow(
                () -> new NotFoundException("Cant find tenant with id: " + dto.tenantId() + " for new rent start."));
        trd.setTenant(t);

        RentalSpace rs = rentalSpaceRepository.findById(dto.rentalSpaceId()).orElseThrow(
                () -> new NotFoundException("Cant find rental space with id: " + dto.rentalSpaceId() + " for new rent start."));
        trd.setRentalSpace(rs);

        if (rs.getOccupant() != null)
        {
            throw new ValidationException("Rental space should already be occupied.");
        }

        trd.setStartDate(dto.startDate());
        trd.addPriceChange(dto.price(), Date.from(Instant.now()));

        trd = tenantRentalRepository.save(trd);
        if (trd == null)
        {
            throw new RuntimeException("error saving new rental data: " + dto);
        }

        t.addRentalData(trd);
        rs.setOccupant(t);

        return trd;
    }
}
