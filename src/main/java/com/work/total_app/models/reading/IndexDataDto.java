package com.work.total_app.models.reading;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode
public class IndexDataDto {
    private Double index;
    private Date readingDate;
    private Long counterId;
    private Double unitPrice; // Price per unit for this reading
}


