package com.work.total_app.utils;

import com.work.total_app.models.file.OwnerType;

import java.util.Map;

public interface OwnerMetadataProvider {
    OwnerType supports();
    Map<String, String> metadataFor(Long ownerId);
}

