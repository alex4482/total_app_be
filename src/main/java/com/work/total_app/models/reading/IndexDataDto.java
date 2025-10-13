package com.work.total_app.models.reading;

import com.work.total_app.models.tenant.TenantRentalData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode
public class IndexDataDto {
    private Double index;
    private Date readingDate;
    private Long counterId;
}


