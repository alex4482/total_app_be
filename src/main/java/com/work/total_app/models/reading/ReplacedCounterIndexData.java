package com.work.total_app.models.reading;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
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
    private IndexData oldCounterData;
    private Double newCounterInitialIndex;
    private Date replacementDate;
}
