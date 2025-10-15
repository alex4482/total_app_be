package com.work.total_app.controllers;

import com.work.total_app.models.file.TempUploadDto;
import com.work.total_app.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class TempUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping(path="/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<TempUploadDto> uploadTemp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value="batchId", required=false) UUID batchId
    ) throws Exception {
        if (batchId == null) batchId = UUID.randomUUID();
        return fileStorageService.uploadTempBatch(batchId, files);
    }
}
