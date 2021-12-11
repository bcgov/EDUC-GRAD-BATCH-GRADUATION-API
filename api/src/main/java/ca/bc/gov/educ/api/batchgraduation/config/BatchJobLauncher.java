package ca.bc.gov.educ.api.batchgraduation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    private static final String TIME="time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";



    @Scheduled(cron = "0 * 19 * * *")
    public void runRegularGradAlgorithm() throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobException {
        LOGGER.info("Batch Job was started");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "REGALG");
        try {
            jobLauncher.run(graduationBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LOGGER.info("Batch Job was stopped");
    }

    @Scheduled(cron = "0 * 21 * * *")
    public void runTranscriptVerificationReportProcess() throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobException {
        LOGGER.info("Batch Job was started");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "BATCH");
        builder.addString(JOB_TYPE, "TVRRUN");
        try {
            jobLauncher.run(tvrBatchJob, builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LOGGER.info("Batch Job was stopped");
    }
}
