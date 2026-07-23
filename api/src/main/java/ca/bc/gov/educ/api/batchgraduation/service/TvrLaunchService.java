package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TvrLaunchService {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER = "jobTrigger";
    private static final String BATCH_TRIGGER = "BATCH";
    private static final String JOB_TYPE = "jobType";
    private static final String ERROR_MSG = "Error {}";

    private final Job tvrBatchJob;
    private final JobLauncher jobLauncher;
    private final GradDashboardService gradDashboardService;

    public TvrLaunchService(
            @Qualifier("tvrBatchJob") Job tvrBatchJob,
            @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
            GradDashboardService gradDashboardService
    ) {
        this.tvrBatchJob = tvrBatchJob;
        this.jobLauncher = jobLauncher;
        this.gradDashboardService = gradDashboardService;
    }

    public void launchTVRReportProcess() {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, BATCH_TRIGGER);
        builder.addString(JOB_TYPE, "TVRRUN");
        Optional<BatchProcessingEntity> bPresent = gradDashboardService.findBatchProcessing("TVRRUN");
        if (bPresent.isPresent() && bPresent.get().getEnabled().equalsIgnoreCase("Y")) {
            try {
                jobLauncher.run(tvrBatchJob, builder.toJobParameters());
            } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                     | JobParametersInvalidException | IllegalArgumentException e) {
                log.error(ERROR_MSG, e.getLocalizedMessage());
            }
        }
    }
}
