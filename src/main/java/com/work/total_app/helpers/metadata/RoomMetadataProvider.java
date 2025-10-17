package com.work.total_app.helpers.metadata;

import com.work.total_app.models.building.Building;
import com.work.total_app.models.building.Room;
import com.work.total_app.models.file.OwnerType;
import com.work.total_app.models.runtime_errors.NotFoundException;
import com.work.total_app.repositories.RoomRepository;
import com.work.total_app.services.BuildingService;
import com.work.total_app.utils.OwnerMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoomMetadataProvider implements OwnerMetadataProvider {
    private final RoomRepository roomRepository;
    private final BuildingService buildingService;
    @Override public OwnerType supports() { return OwnerType.ROOM; }
    @Override public Map<String, String> metadataFor(Long ownerId) {
        Room r = roomRepository.findById(ownerId).orElseThrow(() -> new NotFoundException("room not found"));
        Building b = buildingService.getBuilding(r.getBuilding().getId());
        return Map.of(
                "roomName", r.getName(),
                "buildingName", b.getName(),
                "buildingLocation", b.getLocation().name()
        );
    }
}
