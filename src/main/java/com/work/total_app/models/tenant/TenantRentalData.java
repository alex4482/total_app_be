package com.work.total_app.models.tenant;

import com.work.total_app.models.reading.IndexData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class TenantRentalData {
    @Id
    @GeneratedValue
    private Long id;

    private String tenantId;
    private String rentalSpaceId;
    private Date startDate;
    private Date endDate;
    @ElementCollection
    private List<IndexData> counterReadings;

    private Double price;
    @ElementCollection
    private List<PriceData> priceChanges = new ArrayList<>();

    // TODO: program a future price change, or ask to validate it
    public void addPriceChange(Double newPrice, Date startDate)
    {
        priceChanges.add(new PriceData(newPrice, startDate));
    }
}

@Data
@Embeddable
@AllArgsConstructor
class PriceData {
    private Double newPrice;
    private Date changeTime;
}
