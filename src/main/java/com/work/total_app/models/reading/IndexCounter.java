package com.work.total_app.models.reading;

import com.work.total_app.models.building.BuildingLocation;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode
public class IndexCounter {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String locationId;

    @Enumerated(EnumType.STRING)
    private CounterType counterType;

    @Enumerated(EnumType.STRING)
    private IndexCounterLocationType locationType;

    @Enumerated(EnumType.STRING)
    private BuildingLocation buildingLocation;

    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexData> indexData = new ArrayList<>();


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
}



