package com.work.total_app.models.tenant;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.work.total_app.models.building.RentalSpace;
import com.work.total_app.models.reading.IndexData;
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference("tenant-rental")
    private Tenant tenant;

    // ONE rental data entry is about ONE rental space
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JsonBackReference("rental-agreement")
    private RentalSpace rentalSpace;

    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Temporal(TemporalType.DATE)
    private Date endDate;

    private Double rent;

    @ElementCollection
    private List<PriceData> priceChanges = new ArrayList<>();

    // utility
    public void addPriceChange(Double newPrice, Date startDate) {
        priceChanges.add(new PriceData(newPrice, startDate));
    }
}

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
class PriceData {
    private Double newPrice;

    @Temporal(TemporalType.DATE)
    private Date changeTime;
}