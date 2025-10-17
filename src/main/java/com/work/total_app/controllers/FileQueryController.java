package com.work.total_app.controllers;

import com.work.total_app.helpers.files.DatabaseHelper;
import com.work.total_app.models.file.FileDto;
import com.work.total_app.models.file.OwnerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileQueryController {

    @Autowired
    private DatabaseHelper databaseHelper;

    @GetMapping
    public List<FileDto> listByOwner(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") Long ownerId
    ) {
        var type = OwnerType.valueOf(ownerType);
        return databaseHelper.listByOwner(type, ownerId).stream()
                .map(f -> new FileDto(
                        f.getId().toString(),
                        f.getOwnerType().name(),
                        f.getOwnerId(),
                        f.getOriginalFilename(),
                        f.getContentType(),
                        f.getSizeBytes(),
                        f.getChecksum(),
                        "/api/files/" + f.getId()
                ))
                .toList();
    }
}
