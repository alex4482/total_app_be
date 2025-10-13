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
    private String subject;
    private String message;
    private String[] recipients;
    private String[] keywords;
}
