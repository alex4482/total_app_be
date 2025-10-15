package com.work.total_app.utils;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageHelper {
    Path save(Long ownerId, String filename, byte[] data) throws IOException;
    byte[] read(Path path) throws IOException;
    void delete(Path path) throws IOException;
}
