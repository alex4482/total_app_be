package com.work.total_app.models.tenant;

import com.work.total_app.models.Observation;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
public class Tenant {
    @Id
    private String id; //same as name but lowercase
    @NonNull
    private String name;
    private String cui;
    private boolean pf;
    private boolean active;
    private List<String> emails;
    @ElementCollection
    private List<Observation> observations;
    private List<String> attachmentIds;
}
