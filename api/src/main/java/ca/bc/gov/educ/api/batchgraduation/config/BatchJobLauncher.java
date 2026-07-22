package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.service.BatchLaunchService;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.service.TvrLaunchService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This bean schedules and runs our Spring Batch job.
 */
@Slf4j
@Component
public class BatchJobLauncher {

    private final Job userScheduledBatchJobRefresher;
    private final JobLauncher jobLauncher;
    private final GradDashboardService gradDashboardService;
    private final BatchLaunchService batchLaunchService;
    private final TvrLaunchService tvrLaunchService;

    @Autowired
    public BatchJobLauncher(
            @Qualifier("userScheduledBatchJobRefresher") Job userScheduledBatchJobRefresher,
            @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
            GradDashboardService gradDashboardService,
            BatchLaunchService batchLaunchService,
            TvrLaunchService tvrLaunchService
    ) {
        this.userScheduledBatchJobRefresher = userScheduledBatchJobRefresher;
        this.jobLauncher = jobLauncher;
        this.gradDashboardService = gradDashboardService;
        this.batchLaunchService = batchLaunchService;
        this.tvrLaunchService = tvrLaunchService;
    }

    private static final String TIME="time";
    private static final String BATCH_STARTED = "job was started";
    private static final String BATCH_ENDED = "job was stopped";
    private static final String ERROR_MSG = "Error {}";

    @Scheduled(cron = "${batch.regalg.cron}")
    @SchedulerLock(name = "GraduationBatchJob", lockAtLeastFor = "${batch.system.scheduled.routines.lockAtLeastFor}", lockAtMostFor = "${batch.system.scheduled.routines.lockAtMostFor}")
    public void runRegularGradAlgorithm() {
        log.info("scheduled REGALG {}", BATCH_STARTED);
        LockAssert.assertLocked();
        batchLaunchService.launchRegularGradAlgorithm();
        log.info("scheduled REGALG {}", BATCH_ENDED);
    }

    @Scheduled(cron = "${batch.distrun.cron}")
    @SchedulerLock(name = "DistributionBatchJob", lockAtLeastFor = "PT10S", lockAtMostFor = "PT120M")
    public void runMonthlyDistributionProcess() {
        log.info("scheduled monthly distribution {}", BATCH_STARTED);
        LockAssert.assertLocked();
        batchLaunchService.launchDistributionRunBatchJob();
        log.info("scheduled monthly distribution {}", BATCH_ENDED);
    }

    @Scheduled(fixedDelayString = "PT30M")
    @SchedulerLock(name = "userScheduledBatchJobRefresher", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    public void refreshUserScheduledQueue() {
        log.info("user scheduled queue {}", BATCH_STARTED);
        LockAssert.assertLocked();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        try {
            jobLauncher.run(userScheduledBatchJobRefresher, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            log.debug(ERROR_MSG, e.getLocalizedMessage());
        }

        log.info("user scheduled queue {}", BATCH_ENDED);
    }

    @Scheduled(cron = "${batch.purge-old-records.cron}")
    @SchedulerLock(name = "PurgeOldRecordsLock",
            lockAtLeastFor = "PT1H", lockAtMostFor = "PT1H") //midnight job so lock for an hour
    public void purgeOldRecords() {
        LockAssert.assertLocked();
        log.info("purging old batch records {}", BATCH_STARTED);
        try {
            this.gradDashboardService.purgeOldBatchHistoryRecords();
            this.gradDashboardService.purgeOldSpringMetaDataRecords();
        } catch (Exception e) {
            log.error(ERROR_MSG, e.getLocalizedMessage());
        }
    }
}
