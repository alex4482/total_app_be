package com.work.total_app.models.reading;

import com.work.total_app.models.building.BuildingLocation;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
    private CounterType counterType;
    private IndexCounterLocationType locationType;
    private BuildingLocation buildingLocation;

    public static Specification<IndexCounter> byFilter(IndexCounterFilter f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.name() != null && !f.name().isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")),
                        "%" + f.name().trim().toLowerCase() + "%"));
            }
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



