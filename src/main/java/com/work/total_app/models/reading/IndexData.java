package com.work.total_app.models.reading;

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
    private Date readingDate;

    private IndexCounter indexCounter;
}

