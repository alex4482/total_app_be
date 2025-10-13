package com.work.total_app.models.reading;

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

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class IndexCounter {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @ManyToOne(fetch = LAZY) @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    private CounterType counterType;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    @Enumerated(EnumType.STRING)
    private BuildingLocation buildingLocation;

    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("readingDate DESC, id ASC")
    private List<IndexData> indexData = new ArrayList<>();

    public void addIndexData(IndexData data) {
        int i = 0;
        while (indexData.get(i).getReadingDate().after(data.getReadingDate()))
        {
            i++;
        }
        indexData.add(i, data);
        data.setConsumption(data.getIndex() - indexData.get(0).getIndex());
        data.setCounter(this);
    }

    public static Specification<IndexCounter> byFilter(IndexCounterFilter f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.locationId() != null && !f.locationId().isBlank()) {
                p.add(cb.equal(root.get("locationId"), f.locationId().trim()));
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

        return ic;
    }
}



