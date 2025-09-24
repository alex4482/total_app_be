package com.work.total_app.models.email;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class EmailFileKeywordPair {
    @Id
    private Long id;
    private String address;
    private String fileKeyword;
}
