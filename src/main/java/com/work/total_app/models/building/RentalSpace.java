package com.work.total_app.models.building;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.tenant.Tenant;
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
public class RentalSpace extends Room {

    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JsonManagedReference("rental-agreement")
    private TenantRentalData rentalAgreement;

    public static Specification<RentalSpace> byFilter(RentalSpaceFilter f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.buildingId() != null && !f.buildingId().isBlank())
            {
                p.add(cb.equal(root.get("buildingId"), f.buildingId().trim()));
            }
            else if (f.buildingLocation() != null){
                p.add(cb.equal(root.get("buildingLocation"), f.buildingLocation()));
            }

            if (f.empty() != null && f.empty()) {
                p.add(cb.equal(root.get("tenant_id"), null));
            }
            else if (f.empty() != null && f.tenantId() != null && !f.tenantId().isBlank())
            {
                p.add(cb.equal(root.get("tenant_id"), f.tenantId()));
            }
            else if (f.empty() != null && !f.empty()){
                p.add(cb.notEqual(root.get("tenant_id"), null));
            }

            if (f.groundLevel() != null) {
                p.add(cb.equal(root.get("groundLevel"), f.groundLevel()));
            }

            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
