package com.work.total_app.helpers.metadata;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.Room;
import com.work.total_app.models.file.OwnerType;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.repositories.RoomRepository;
import com.work.total_app.services.BuildingService;
import com.work.total_app.services.TenantService;
import com.work.total_app.utils.OwnerMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TenantMetadataProvider implements OwnerMetadataProvider {
    private final TenantService tenantService; // serviciul tău
    @Override public OwnerType supports() { return OwnerType.TENANT; }
    @Override public Map<String, String> metadataFor(Long ownerId) {
        var t = tenantService.getTenant(ownerId);
        return Map.of("tenantName", t.getName());
    }
}

