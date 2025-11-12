package com.work.total_app.repositories;

import com.work.total_app.models.tenant.ServiceMonthlyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceMonthlyValueRepository extends JpaRepository<ServiceMonthlyValue, Long> {
    /**
     * Find custom value for a service in a specific month.
     */
    Optional<ServiceMonthlyValue> findByRentalDataIdAndServiceIdAndYearAndMonth(
        Long rentalDataId, Long serviceId, Integer year, Integer month);
    
    /**
     * Find all custom values for a rental agreement.
     */
    List<ServiceMonthlyValue> findByRentalDataId(Long rentalDataId);
    
    /**
     * Find all custom values for a rental agreement and year.
     */
    List<ServiceMonthlyValue> findByRentalDataIdAndYear(Long rentalDataId, Integer year);
    
    /**
     * Delete all custom values for a rental agreement.
     */
    void deleteByRentalDataId(Long rentalDataId);
}

