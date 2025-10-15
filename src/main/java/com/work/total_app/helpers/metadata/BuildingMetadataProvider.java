package com.work.total_app.helpers.metadata;

import com.work.total_app.models.file.OwnerType;
import com.work.total_app.services.BuildingService;
import com.work.total_app.utils.OwnerMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BuildingMetadataProvider implements OwnerMetadataProvider {
    private final BuildingService buildingService;
    @Override public OwnerType supports() { return OwnerType.BUILDING; }
    @Override public Map<String, String> metadataFor(String ownerId) {
        var b = buildingService.getBuilding(ownerId);
        return Map.of(
                "buildingName", b.getName(),
                "buildingLocation", b.getLocation().name() // are 2 valori, cum ai spus
        );
    }
}
