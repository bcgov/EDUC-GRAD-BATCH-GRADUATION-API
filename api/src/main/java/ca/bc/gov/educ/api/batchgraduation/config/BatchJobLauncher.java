package ca.bc.gov.educ.api.batchgraduation.config;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This bean schedules and runs our Spring Batch job.
 */
@PropertySource("classpath:/batch.properties")
@Component
public class BatchJobLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchJobLauncher.class);

    @Value("${batch.regular.grad.job.enabled}")
    private boolean regularGradJobEnabled;

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

    private static final String TIME="time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";



    @Scheduled(cron = "${batch.regular.grad.job.cron}")
    @SchedulerLock(name = "GraduationBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runRegularGradAlgorithm() {
        LOGGER.info("Batch Job was started {}",regularGradJobEnabled);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "REGALG");
        try {
            jobLauncher.run(graduationBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            LOGGER.debug("Error {}",e.getLocalizedMessage());
        }
        LOGGER.info("Batch Job was stopped");
    }

    @Scheduled(cron = "0 0 20 * * *")
    @SchedulerLock(name = "tvrBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runTranscriptVerificationReportProcess() {
        LOGGER.info("Batch Job was started");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "TVRRUN");

        try {
            jobLauncher.run(tvrBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            LOGGER.debug("Error {}",e.getLocalizedMessage());
        }
        LOGGER.info("Batch Job was stopped");
    }

    @Scheduled(cron = "0 0 23 * * *")
    @SchedulerLock(name = "SchoolReportBatchJob", lockAtLeastFor = "10s", lockAtMostFor = "120m")
    public void runSchoolReportPosting() {
        LOGGER.info("Batch Job was started");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "SCHREP");
        try {
            jobLauncher.run(schoolReportBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            LOGGER.debug("Error {}",e.getLocalizedMessage());
        }
        LOGGER.info("Batch Job was stopped");
    }
}
