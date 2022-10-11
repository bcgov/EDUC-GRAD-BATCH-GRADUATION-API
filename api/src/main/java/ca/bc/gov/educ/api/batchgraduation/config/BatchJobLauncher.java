package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This bean schedules and runs our Spring Batch job.
 */
@Component
public class BatchJobLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchJobLauncher.class);

    @Autowired
    @Qualifier("GraduationBatchJob")
    private Job graduationBatchJob;

    @Autowired
    @Qualifier("tvrBatchJob")
    private Job tvrBatchJob;

    @Autowired
    @Qualifier("DistributionBatchJob")
    private Job distributionBatchJob;

    @Autowired
    @Qualifier("userScheduledBatchJobRefresher")
    private Job userScheduledBatchJobRefresher;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private GradDashboardService gradDashboardService;

    private static final String TIME="time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String BATCH_TRIGGER = "BATCH";
    private static final String JOB_TYPE="jobType";
    private static final String BATCH_STARTED = "Batch Job was started";
    private static final String BATCH_ENDED = "Batch Job was stopped";
    private static final String ERROR_MSG = "Error {}";

    @Scheduled(cron = "${batch.regalg.cron}")
    @SchedulerLock(name = "GraduationBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runRegularGradAlgorithm() {
        LOGGER.info(BATCH_STARTED);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, BATCH_TRIGGER);
        builder.addString(JOB_TYPE, "REGALG");
        Optional<BatchProcessingEntity> bPresent = gradDashboardService.findBatchProcessing("REGALG");
        if(bPresent.isPresent() && bPresent.get().getEnabled().equalsIgnoreCase("Y")) {
            try {
                jobLauncher.run(graduationBatchJob, builder.toJobParameters());
            } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                    | JobParametersInvalidException | IllegalArgumentException e) {
                LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
            }
        }
        LOGGER.info(BATCH_ENDED);
    }

    @Scheduled(cron = "${batch.tvrrun.cron}")
    @SchedulerLock(name = "tvrBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runTranscriptVerificationReportProcess() {
        LOGGER.info(BATCH_STARTED);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, BATCH_TRIGGER);
        builder.addString(JOB_TYPE, "TVRRUN");
        Optional<BatchProcessingEntity> bPresent = gradDashboardService.findBatchProcessing("TVRRUN");
        if(bPresent.isPresent() && bPresent.get().getEnabled().equalsIgnoreCase("Y")) {
            try {
                jobLauncher.run(tvrBatchJob, builder.toJobParameters());
            } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                    | JobParametersInvalidException | IllegalArgumentException e) {
                LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
            }
        }
        LOGGER.info(BATCH_ENDED);
    }

    @Scheduled(cron = "${batch.distrun.cron}")
    @SchedulerLock(name = "DistributionBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runMonthlyDistributionProcess() {
        LOGGER.info(BATCH_STARTED);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, BATCH_TRIGGER);
        builder.addString(JOB_TYPE, "DISTRUN");
        Optional<BatchProcessingEntity> bPresent = gradDashboardService.findBatchProcessing("DISTRUN");
        if(bPresent.isPresent() && bPresent.get().getEnabled().equalsIgnoreCase("Y")) {
            try {
                jobLauncher.run(distributionBatchJob, builder.toJobParameters());
            } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                    | JobParametersInvalidException | IllegalArgumentException e) {
                LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
            }
        }
        LOGGER.info(BATCH_ENDED);
    }

    @Scheduled(fixedDelayString = "PT30M")
    public void refreshUserScheduledQueue() {
        LOGGER.info(BATCH_STARTED);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        try {
            jobLauncher.run(userScheduledBatchJobRefresher, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
        }

        LOGGER.info(BATCH_ENDED);
    }
}
