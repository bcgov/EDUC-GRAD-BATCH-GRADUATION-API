package ca.bc.gov.educ.api.batchgraduation.config;

import net.javacrumbs.shedlock.core.*;
import net.javacrumbs.shedlock.spring.LockableTaskScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.*;
import java.util.Optional;

@Configuration
public class TaskSchedulerConfig {

    @Autowired
    LockProvider lockProvider;

    @Bean(name="lockableTaskScheduler")
    public LockableTaskScheduler getScheduler() {
        // ShedLock config: lockAtMostFor = 60 sec, lockAtLeastFor = 1 sec
        LockConfigurationExtractor lockConfigurationExtractor = (task) ->  Optional.
                of(new LockConfiguration(LocalDateTime.now().toInstant(ZoneOffset.UTC), "userScheduledJob", Duration.ofSeconds(60), Duration.ofSeconds(1)));

        LockManager lockManager = new DefaultLockManager(lockProvider, lockConfigurationExtractor);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("UserScheduledTaskScheduler");
        scheduler.initialize();
        return new LockableTaskScheduler(scheduler, lockManager);
    }
}
