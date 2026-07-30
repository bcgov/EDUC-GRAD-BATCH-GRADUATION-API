package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.config.BatchJobLauncher;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class SystemBatchSchedulingService {

    private static final String REGALG = "REGALG";

    private final BatchProcessingRepository batchProcessingRepository;
    private final ThreadPoolTaskScheduler systemTaskScheduler;
    private final BatchJobLauncher batchJobLauncher;
    private final Map<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    public SystemBatchSchedulingService(
            BatchProcessingRepository batchProcessingRepository,
            @Qualifier("systemTaskScheduler") ThreadPoolTaskScheduler systemTaskScheduler,
            BatchJobLauncher batchJobLauncher) {
        this.batchProcessingRepository = batchProcessingRepository;
        this.systemTaskScheduler = systemTaskScheduler;
        this.batchJobLauncher = batchJobLauncher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSystemSchedules() {
        refreshScheduledJob(REGALG);
    }

    public void refreshScheduledJob(String jobType) {
        // only regalg can be rescheduled currently
        // other jobs still rely on static @Scheduled, see BatchJobLauncher.java
        if (!REGALG.equalsIgnoreCase(jobType)) {
            return;
        }
        Optional<BatchProcessingEntity> scheduleOpt =
                Optional.ofNullable(batchProcessingRepository.findByJobType(REGALG)).orElse(Optional.empty());
        cancelScheduledJob(REGALG);
        if (scheduleOpt.isEmpty()) {
            log.info("No {} schedule found to initialize", REGALG);
            return;
        }
        BatchProcessingEntity schedule = scheduleOpt.get();
        if (!"Y".equalsIgnoreCase(schedule.getEnabled()) || schedule.getCronExpression() == null || schedule.getCronExpression().isBlank()) {
            log.info("{} schedule is disabled or empty; nothing to schedule", REGALG);
            return;
        }

        ScheduledFuture<?> future = systemTaskScheduler.schedule(
                batchJobLauncher::runRegularGradAlgorithm,
                new CronTrigger(schedule.getCronExpression(), TimeZone.getTimeZone(TimeZone.getDefault().getID()))
        );
        scheduledJobs.put(REGALG, future);
        log.info("{} schedule refreshed with cron {}", REGALG, schedule.getCronExpression());
    }

    public void cancelScheduledJob(String jobType) {
        ScheduledFuture<?> future = scheduledJobs.remove(jobType.toUpperCase());
        if (future != null) {
            future.cancel(false);
            log.info("{} schedule cancelled", jobType.toUpperCase());
        }
    }
}
