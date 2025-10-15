package com.work.total_app.jobs;

import com.work.total_app.helpers.files.DatabaseHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
public class TempCleanupJob {

    @Autowired
    private DatabaseHelper databaseHelper;

    @Scheduled(cron = "0 0 */2 * * *")
    public void cleanup() {
        var now = Instant.now();
        var expired = databaseHelper.findExpiredTemp(now);
        for (var tu : expired) {
            try {
                Files.deleteIfExists(Path.of(tu.getTempPath()));
            } catch (Exception ignored) {}
            databaseHelper.deleteTemp(tu.getId());
        }
    }
}
