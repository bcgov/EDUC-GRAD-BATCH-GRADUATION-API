package ca.bc.gov.educ.api.batchgraduation.config;

import net.javacrumbs.shedlock.core.*;
import net.javacrumbs.shedlock.spring.LockableTaskScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.*;
import java.util.Optional;

/**
 * Lockable Task Scheduler for User Scheduled Jobs
 * Managed by ShedLock
 */
@Configuration
public class TaskSchedulerConfig {

    @Autowired
    LockProvider lockProvider;

    // Shedlock configuration for user scheduled jobs
    @Value("${batch.user.scheduled.jobs.lockAtLeastFor}")
    private int userScheduledJobsLockAtLeastFor;

    @Value("${batch.user.scheduled.jobs.lockAtMostFor}")
    private int userScheduledJobsLockAtMostFor;

    @Bean(name="lockableTaskScheduler")
    public LockableTaskScheduler getScheduler() {
        // ShedLock config
        LockConfigurationExtractor lockConfigurationExtractor = (task) ->  Optional.
                of(new LockConfiguration(LocalDateTime.now().toInstant(ZoneOffset.UTC), "userScheduledJob", Duration.ofSeconds(userScheduledJobsLockAtMostFor), Duration.ofSeconds(userScheduledJobsLockAtLeastFor)));

        LockManager lockManager = new DefaultLockManager(lockProvider, lockConfigurationExtractor);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("UserScheduledTask-");
        scheduler.initialize();
        return new LockableTaskScheduler(scheduler, lockManager);
    }
}
