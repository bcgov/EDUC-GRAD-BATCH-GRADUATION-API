package ca.bc.gov.educ.api.batchgraduation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
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

import java.util.Set;

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
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExplorer jobExplorer;

    private static final String TIME="time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";



    @Scheduled(cron = "0 0 18 * * *")
    public void runRegularGradAlgorithm() {
        LOGGER.info("Batch Job was started");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "REGALG");
        try {
            if (isJobRunning(graduationBatchJob)) {
                return;
            }
            jobLauncher.run(graduationBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            LOGGER.debug("Error {}",e.getLocalizedMessage());
        }
        LOGGER.info("Batch Job was stopped");
    }

    @Scheduled(cron = "0 0 23 * * *")
    public void runTranscriptVerificationReportProcess() {
        LOGGER.info("Batch Job was started");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "TVRRUN");

        try {
            if (isJobRunning(tvrBatchJob)) {
                return;
            }
            jobLauncher.run(tvrBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | IllegalArgumentException e) {
            LOGGER.debug("Error {}",e.getLocalizedMessage());
        }
        LOGGER.info("Batch Job was stopped");
    }

    private boolean isJobRunning(Job job) {
        Set<JobExecution> jobExecutions = jobExplorer.findRunningJobExecutions(job.getName());
        return !jobExecutions.isEmpty();
    }
}
