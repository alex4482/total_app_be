package com.work.total_app.services;

import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.models.runtime_errors.ValidationException;
import com.work.total_app.models.tenant.*;
import com.work.total_app.repositories.RentalSpaceRepository;
import com.work.total_app.repositories.TenantRentalDataRepository;
import com.work.total_app.repositories.TenantRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@Log4j2
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

        if (rs.getRentalAgreement() != null)
        {
            throw new ValidationException("Rental space is already occupied. Please select a different space.");
        }

        // Validate end date if provided
        if (dto.endDate() != null && dto.startDate() != null && dto.endDate().before(dto.startDate())) {
            throw new ValidationException(
                String.format("End date (%s) cannot be before start date (%s)", 
                    dto.endDate(), dto.startDate()));
        }

        trd.setStartDate(dto.startDate());
        trd.setEndDate(dto.endDate());
        trd.setRent(dto.price());
        trd.setCurrency(dto.currency() != null ? dto.currency() : Currency.RON);
        trd.addPriceChange(dto.price(), dto.startDate() != null ? dto.startDate() : Date.from(Instant.now()));

        trd = tenantRentalRepository.save(trd);

        t.addRentalData(trd);
        rs.setRentalAgreement(trd);
        rentalSpaceRepository.save(rs);
        tenantRepository.save(t);

        return trd;
    }

    /**
     * Update rental agreement details.
     */
    @Transactional
    public TenantRentalData updateRentalAgreement(Long rentalAgreementId, UpdateTenantRentalDto dto) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        if (dto.startDate() != null) {
            trd.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            trd.setEndDate(dto.endDate());
        }
        if (dto.rent() != null) {
            trd.setRent(dto.rent());
        }
        if (dto.currency() != null) {
            trd.setCurrency(dto.currency());
        }

        return tenantRentalRepository.save(trd);
    }

    /**
     * Change rental price with effective date.
     * Adds a new entry to priceChanges history and updates current rent.
     */
    @Transactional
    public TenantRentalData changePrice(Long rentalAgreementId, ChangePriceDto dto) {
        if (dto.newPrice() == null || dto.newPrice() <= 0) {
            throw new ValidationException("New price must be greater than 0");
        }
        if (dto.effectiveDate() == null) {
            throw new ValidationException("Effective date is required");
        }

        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Validate effective date is between startDate and endDate (if exists)
        if (trd.getStartDate() != null && dto.effectiveDate().before(trd.getStartDate())) {
            throw new ValidationException(
                String.format("Effective date (%s) cannot be before rental start date (%s)", 
                    dto.effectiveDate(), trd.getStartDate()));
        }
        if (trd.getEndDate() != null && dto.effectiveDate().after(trd.getEndDate())) {
            throw new ValidationException(
                String.format("Effective date (%s) cannot be after rental end date (%s)", 
                    dto.effectiveDate(), trd.getEndDate()));
        }

        // Update current rent
        trd.setRent(dto.newPrice());

        // Add to price change history
        trd.addPriceChange(dto.newPrice(), dto.effectiveDate());

        log.info("Changed price for rental agreement {} to {} effective from {}", 
                rentalAgreementId, dto.newPrice(), dto.effectiveDate());

        return tenantRentalRepository.save(trd);
    }

    /**
     * Terminate rental agreement by setting end date.
     */
    @Transactional
    public TenantRentalData terminateRentalAgreement(Long rentalAgreementId, TerminateRentalDto dto) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        if (dto.endDate() == null) {
            throw new ValidationException("End date is required");
        }

        // Validate end date
        if (trd.getStartDate() != null && dto.endDate().before(trd.getStartDate())) {
            throw new ValidationException(
                String.format("End date (%s) cannot be before start date (%s)", 
                    dto.endDate(), trd.getStartDate()));
        }

        trd.setEndDate(dto.endDate());

        // Log message based on whether end date is in the future or already reached
        Date now = Date.from(Instant.now());
        if (dto.endDate().after(now)) {
            log.info("Scheduled future termination for rental agreement {} with end date {}", rentalAgreementId, dto.endDate());
        } else {
            log.info("Terminated rental agreement {} with end date {}", rentalAgreementId, dto.endDate());
        }

        return tenantRentalRepository.save(trd);
    }

    /**
     * Delete rental agreement completely (removes link between tenant and rental space).
     */
    @Transactional
    public void deleteRentalAgreement(Long rentalAgreementId) {
        TenantRentalData trd = tenantRentalRepository.findById(rentalAgreementId)
                .orElseThrow(() -> new NotFoundException("Rental agreement not found with id: " + rentalAgreementId));

        // Remove from tenant
        Tenant tenant = trd.getTenant();
        if (tenant != null) {
            tenant.removeRentalData(trd);
            tenantRepository.save(tenant);
        }

        // Remove from rental space
        RentalSpace rentalSpace = trd.getRentalSpace();
        if (rentalSpace != null) {
            rentalSpace.setRentalAgreement(null);
            rentalSpaceRepository.save(rentalSpace);
        }

        // Delete the rental agreement
        tenantRentalRepository.delete(trd);

        log.info("Deleted rental agreement {}", rentalAgreementId);
    }
}
