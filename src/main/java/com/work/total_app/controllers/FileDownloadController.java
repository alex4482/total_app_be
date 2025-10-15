package com.work.total_app.controllers;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.helpers.files.FileSystemHelper;
import com.work.total_app.models.file.FileAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    @Autowired
    private DatabaseHelper databaseHelper;
    @Autowired
    private FileSystemHelper fileSystemHelper;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadById(@PathVariable UUID id) throws Exception {
        FileAsset f = databaseHelper.findById(id).orElseThrow();
        // NOTE: For simplicity we always use BLOB here. You can also reconstruct FS path if you store it.
        String filename = f.getOriginalFilename();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String contentType = f.getContentType() != null ? f.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        byte[] body = f.getData(); // could be null if you disabled BLOBs
        if (body == null) {
            // If BLOB is null, try to read from FS by scanning expected folder (not implemented here).
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(f.getSizeBytes())
                .body(body);
    }
}
