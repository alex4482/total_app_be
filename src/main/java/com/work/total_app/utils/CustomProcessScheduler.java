package com.work.total_app.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Log4j2
@Component
public class CustomProcessScheduler extends ThreadPoolTaskScheduler {

    private final Map<Object, ScheduledFuture<?>> scheduledTasks = new IdentityHashMap<>();

    public void cancelProcess(Object key)
    {
        if (!scheduledTasks.containsKey(key))
        {
            return;
        }
        log.info("Canceled scheduled task with key <{}>", key);
        scheduledTasks.get(key).cancel(true);
    }

    public void runProcess(Object key, Runnable process, Instant startTime, Duration delay)
    {
        if (scheduledTasks.containsKey(key))
        {
            log.error("Scheduled process already exists {}", key.toString());
        }
        log.info("Start scheduled task at instant <{}> with delay <{}> with key <{}>", startTime, delay, key);
        scheduledTasks.put(key, scheduleWithFixedDelay(process, startTime, delay));
    }
}