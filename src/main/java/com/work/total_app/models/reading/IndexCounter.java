package com.work.total_app.models.reading;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.work.total_app.models.building.BuildingLocation;
import com.work.total_app.models.building.Location;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.EAGER;

@Entity
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class IndexCounter {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @ManyToOne(fetch = EAGER) @JoinColumn(name = "location_id")
    @JsonBackReference("location-counters") // Prevent circular reference with Location.counters
    private Location location;

    @Enumerated(EnumType.STRING)
    private CounterType counterType;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    @Enumerated(EnumType.STRING)
    private BuildingLocation buildingLocation;

    // Default/Global unit price for this counter
    // If an IndexData doesn't have a local unitPrice, it will use this
    private Double defaultUnitPrice;

    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("readingDate DESC, id ASC")
    @JsonManagedReference // Allow serialization of this side of the relationship
    private List<IndexData> indexData = new ArrayList<>();

    public void addIndexData(IndexData data) {
        data.setCounter(this);
        
        if (indexData.isEmpty()) {
            // First reading - no consumption yet
            data.setConsumption(0.0);
            indexData.add(data);
            return;
        }
        
        // Find correct position to insert (ordered by date DESC)
        int i = 0;
        while (i < indexData.size() && indexData.get(i).getReadingDate().after(data.getReadingDate())) {
            i++;
        }
        indexData.add(i, data);
        
        // Calculate consumption: new reading - previous reading
        if (i + 1 < indexData.size()) {
            // There's a previous (older) reading
            IndexData previousReading = indexData.get(i + 1);
            data.setConsumption(data.getIndex() - previousReading.getIndex());
        } else {
            // This is now the oldest reading
            data.setConsumption(0.0);
        }
        
        // Calculate total cost for current reading using effective price
        Double effectivePrice = data.getEffectiveUnitPrice();
        if (effectivePrice != null && data.getConsumption() != null) {
            data.setTotalCost(data.getConsumption() * effectivePrice);
        }
        
        // Recalculate consumption for the next reading (if exists)
        if (i > 0) {
            IndexData nextReading = indexData.get(i - 1);
            nextReading.setConsumption(nextReading.getIndex() - data.getIndex());
            Double nextEffectivePrice = nextReading.getEffectiveUnitPrice();
            if (nextEffectivePrice != null) {
                nextReading.setTotalCost(nextReading.getConsumption() * nextEffectivePrice);
            }
        }
    }

    public static Specification<IndexCounter> byFilter(IndexCounterFilter f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.locationId() != null && !f.locationId().isBlank()) {
                // Access nested location.id instead of non-existent locationId
                p.add(cb.equal(root.get("location").get("id"), Long.parseLong(f.locationId().trim())));
            }
            if (f.counterType() != null) {
                p.add(cb.equal(root.get("counterType"), f.counterType()));
            }
            if (f.locationType() != null) {
                p.add(cb.equal(root.get("locationType"), f.locationType()));
            }
            if (f.buildingLocation() != null) {
                p.add(cb.equal(root.get("buildingLocation"), f.buildingLocation()));
            }

            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    public static IndexCounter fromDto(IndexCounterDto dto, Location l)
    {
        IndexCounter ic = new IndexCounter();
        ic.setLocation(l);
        ic.setCounterType(dto.getCounterType());
        ic.setName(dto.getName());
        ic.setIndexData(new ArrayList<>());
        ic.setBuildingLocation(dto.getBuildingLocation());
        ic.setLocationType(dto.getLocationType());
        ic.setDefaultUnitPrice(dto.getDefaultUnitPrice());

        return ic;
    }
}



