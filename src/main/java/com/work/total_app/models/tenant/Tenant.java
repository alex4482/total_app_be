package com.work.total_app.models.tenant;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.Observation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Tenant {
    @Id
    @GeneratedValue
    private Long id; //same as name but lowercase
    @NonNull
    private String name;
    private String cui;
    private String regNumber;
    private Boolean pf;
    private Boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> emails = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> phoneNumbers = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Observation> observations = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> attachmentIds = new ArrayList<>();

    // Tenant ↔ TenantRentalData
    @OneToMany(mappedBy = "tenant", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("tenant-rental")
    private List<TenantRentalData> rentalData = new ArrayList<>();

    public void addRentalData(TenantRentalData rd) {
        rentalData.add(rd);
        rd.setTenant(this);
    }

    public void removeRentalData(TenantRentalData rd) {
        rentalData.remove(rd);
        rd.setTenant(null);
    }

    public void getDataFromDto(CreateTenantDto tenantDto) {
        setName(tenantDto.getName());
        setPf(tenantDto.getPf());
        setCui(tenantDto.getCui());
        setRegNumber(tenantDto.getRegNumber());
        setObservations(tenantDto.getObservations());
        setEmails(tenantDto.getEmails());
        setPhoneNumbers(tenantDto.getPhoneNumbers());
    }
}
