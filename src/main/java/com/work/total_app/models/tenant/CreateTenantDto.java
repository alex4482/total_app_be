package com.work.total_app.models.tenant;

import com.work.total_app.models.Observation;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTenantDto {

    private String id; //same as name but lowercase
    private String name;
    private String cui;
    private Boolean pf;
    private Boolean active;

    private List<String> emails;
    private List<Observation> observations;
}
