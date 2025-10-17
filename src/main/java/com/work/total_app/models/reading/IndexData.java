package com.work.total_app.models.reading;

import com.work.total_app.models.tenant.TenantRentalData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class IndexData {
    @Id
    @GeneratedValue
    private Long id;

    private Double index;
    private Double consumption;

    @Enumerated(EnumType.STRING)
    private CounterType type;

    @Temporal(TemporalType.DATE)
    private Date readingDate;

    // Each IndexData belongs to one counter
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "counter_id")
    private IndexCounter counter;
}


