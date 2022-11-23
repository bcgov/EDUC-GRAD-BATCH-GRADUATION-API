package ca.bc.gov.educ.api.batchgraduation.config;

import net.javacrumbs.shedlock.core.*;
import net.javacrumbs.shedlock.spring.LockableTaskScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Configuration
public class TaskSchedulerConfig {

    @Autowired
    LockProvider lockProvider;

    @Bean(name="lockableTaskScheduler")
    public LockableTaskScheduler getScheduler() {
        // ShedLock config: lockAtMostFor = 4 hours, lockAtLeastFor = 10 seconds
        LockConfigurationExtractor lockConfigurationExtractor = (task) ->  Optional.
                of(new LockConfiguration(Instant.now(), "userScheduledJob", Duration.ofHours(2), Duration.ofSeconds(10)));

        LockManager lockManager = new DefaultLockManager(lockProvider, lockConfigurationExtractor);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("UserScheduledTaskScheduler");
        scheduler.initialize();
        return new LockableTaskScheduler(scheduler, lockManager);
    }
}
