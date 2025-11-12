package com.work.total_app.models.tenant;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.work.total_app.models.building.RentalSpace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class TenantRentalData {
    @Id
    @GeneratedValue
    private Long id;

    // MANY rental data entries belong to ONE tenant
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference("tenant-rental")
    private Tenant tenant;

    // ONE rental data entry is about ONE rental space
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JsonBackReference("rental-agreement")
    private RentalSpace rentalSpace;

    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date endDate;

    private Double rent;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency = Currency.RON;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<PriceData> priceChanges = new ArrayList<>();

    // Contract information (optional)
    private String contractNumber;
    
    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private Date contractDate;

    // Active services for this rental agreement
    @ElementCollection(fetch = FetchType.EAGER)
    private List<ActiveService> activeServices = new ArrayList<>();

    // utility
    public void addPriceChange(Double newPrice, Date startDate) {
        priceChanges.add(new PriceData(newPrice, startDate));
    }

    // JSON properties for frontend (without causing circular references)
    @JsonProperty("rentalSpaceId")
    public String getRentalSpaceId() {
        return rentalSpace != null ? rentalSpace.getName() : null;
    }

    @JsonProperty("rentalSpaceName")
    public String getRentalSpaceName() {
        return rentalSpace != null ? rentalSpace.getName() : null;
    }

    @JsonProperty("buildingId")
    public Long getBuildingId() {
        return rentalSpace != null && rentalSpace.getBuilding() != null 
            ? rentalSpace.getBuilding().getId() : null;
    }
}

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
class PriceData {
    private Double newPrice;

    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "dd-mm-yyyy", timezone = "UTC")
    private Date changeTime;
}

