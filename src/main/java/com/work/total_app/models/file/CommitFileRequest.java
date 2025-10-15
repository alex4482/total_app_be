package com.work.total_app.models.file;

import java.util.List;
import java.util.UUID;

public record CommitFileRequest(
        String ownerType,
        String ownerId,
        List<UUID> tempIds,
        boolean overwrite
) {}
