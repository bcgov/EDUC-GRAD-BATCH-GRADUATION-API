package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
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
    @Qualifier("SchoolReportBatchJob")
    private Job schoolReportBatchJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private BatchProcessingRepository batchProcessingRepository;

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
        Optional<BatchProcessingEntity> bPresent = batchProcessingRepository.findByJobType("REGALG");
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
        Optional<BatchProcessingEntity> bPresent = batchProcessingRepository.findByJobType("TVRRUN");
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

    @Scheduled(cron = "${batch.schrep.cron}")
    @SchedulerLock(name = "SchoolReportBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runSchoolReportPosting() {
        LOGGER.info(BATCH_STARTED);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, BATCH_TRIGGER);
        builder.addString(JOB_TYPE, "SCHREP");
        Optional<BatchProcessingEntity> bPresent = batchProcessingRepository.findByJobType("SCHREP");
        if(bPresent.isPresent() && bPresent.get().getEnabled().equalsIgnoreCase("Y")) {
            try {
                jobLauncher.run(schoolReportBatchJob, builder.toJobParameters());
            } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                    | JobParametersInvalidException | IllegalArgumentException e) {
                LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
            }
        }
        LOGGER.info(BATCH_ENDED);
    }
}
