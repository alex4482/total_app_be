package com.work.total_app.models.email;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EmailFileKeywordsDto {
    private List<EmailFileKeywordPair> presets;
}
