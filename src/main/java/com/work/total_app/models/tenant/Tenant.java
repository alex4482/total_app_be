package com.work.total_app.models.tenant;

import com.work.total_app.models.Observation;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.building.RentalSpaceFilter;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Tenant {
    @Id
    private String id; //same as name but lowercase
    @NonNull
    private String name;
    private String cui;
    private Boolean pf;
    private Boolean active;

    @ElementCollection
    private List<String> emails;
    @ElementCollection
    private List<Observation> observations;
    @ElementCollection
    private List<String> attachmentIds;

    // Tenant ↔ TenantRentalData
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantRentalData> rentalData;

    public void addRentalData(TenantRentalData rd) {
        rentalData.add(rd);
        rd.setTenant(this);
    }

    public void removeRentalData(TenantRentalData rd) {
        rentalData.remove(rd);
        rd.setTenant(null);
    }
}
