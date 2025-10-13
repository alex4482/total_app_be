package com.work.total_app.models.reading;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@PrimaryKeyJoinColumn(name = "id")
@Entity
public class ReplacedCounterIndexData extends IndexData {
    // One replaced entry refers to exactly one old IndexData,
    // and each old IndexData can be referenced by at most one replacement
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "old_index_data_id", nullable = false, unique = true)
    private IndexData oldIndexData;
    private Double newCounterInitialIndex;
    @Temporal(TemporalType.DATE)
    private Date replacementDate;
}
