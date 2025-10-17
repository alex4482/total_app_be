package com.work.total_app.helpers.metadata;

import com.work.total_app.models.file.OwnerType;
import com.work.total_app.utils.OwnerMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

// RENTAL_SPACE reutilizează Room (alias)
@Component
@RequiredArgsConstructor
public class RentalSpaceMetadataProvider implements OwnerMetadataProvider {
    private final RoomMetadataProvider roomProvider;
    @Override public OwnerType supports() { return OwnerType.RENTAL_SPACE; }
    @Override public Map<String, String> metadataFor(Long ownerId) {
        return roomProvider.metadataFor(ownerId);
    }
}
