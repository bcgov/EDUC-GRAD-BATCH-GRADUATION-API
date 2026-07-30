package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BatchLaunchService {

    private static final String TIME="time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String BATCH_TRIGGER = "BATCH";
    private static final String JOB_TYPE="jobType";
    private static final String ERROR_MSG = "Error {}";

    private final Job graduationBatchJob;
    private final Job distributionBatchJob;
    private final JobLauncher jobLauncher;
    private final GradDashboardService gradDashboardService;

    public BatchLaunchService(
            @Qualifier("GraduationBatchJob") Job graduationBatchJob,
            @Qualifier("DistributionBatchJob") Job distributionBatchJob,
            @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
            GradDashboardService gradDashboardService) {
        this.graduationBatchJob = graduationBatchJob;
        this.distributionBatchJob = distributionBatchJob;
        this.jobLauncher = jobLauncher;
        this.gradDashboardService = gradDashboardService;
    }

    public void launchRegularGradAlgorithm(){
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
                log.error(ERROR_MSG, e.getLocalizedMessage());
            }
        }
    }

    public void launchDistributionRunBatchJob(){
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
                log.error(ERROR_MSG, e.getLocalizedMessage());
            }
        }
    }

}
