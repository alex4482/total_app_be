package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.tenant.TenantRentalData;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@PrimaryKeyJoinColumn(name = "name")
@DiscriminatorValue("RentalSpace")
public class RentalSpace extends Room {

    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JsonManagedReference("rental-agreement")
    private TenantRentalData rentalAgreement;

    /**
     * Create a RentalSpace instance from DTO.
     */
    public static RentalSpace fromDto(CreateLocationDto dto) {
        RentalSpace rentalSpace = new RentalSpace();
        rentalSpace.populateFromDto(dto);
        rentalSpace.setGroundLevel(dto.getGroundLevel());
        return rentalSpace;
    }

    public static Specification<RentalSpace> byFilter(RentalSpaceFilter f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.buildingId() != null && !f.buildingId().isBlank())
            {
                p.add(cb.equal(root.get("building").get("id"), Long.parseLong(f.buildingId().trim())));
            }
            else if (f.buildingLocation() != null){
                p.add(cb.equal(root.get("location"), f.buildingLocation()));
            }

            // Filter by tenant through rentalAgreement
            if (f.empty() != null && f.empty()) {
                // Empty spaces: no rental agreement
                p.add(cb.isNull(root.get("rentalAgreement")));
            }
            else if (f.empty() != null && f.tenantId() != null && !f.tenantId().isBlank())
            {
                // Specific tenant: join through rentalAgreement -> tenant
                p.add(cb.equal(
                    root.get("rentalAgreement").get("tenant").get("id"), 
                    Long.parseLong(f.tenantId())
                ));
            }
            else if (f.empty() != null && !f.empty()){
                // Occupied spaces: has rental agreement
                p.add(cb.isNotNull(root.get("rentalAgreement")));
            }

            if (f.groundLevel() != null) {
                p.add(cb.equal(root.get("groundLevel"), f.groundLevel()));
            }

            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
