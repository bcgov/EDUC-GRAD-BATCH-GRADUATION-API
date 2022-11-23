package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public abstract class BaseDistributionPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDistributionPartitioner.class);

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    protected abstract JobExecution getJobExecution();

    protected BatchGradAlgorithmJobHistoryEntity createBatchJobHistory() {
        Long jobExecutionId = getJobExecution().getId();
        JobParameters jobParameters = getJobExecution().getJobParameters();
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");
        String studentSearchRequest = jobParameters.getString("searchRequest");
        String status = getJobExecution().getStatus().toString();
        Date startTime = getJobExecution().getStartTime();

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(0L);
        ent.setExpectedStudentsProcessed(0L);
        ent.setFailedStudentsProcessed(0);
        ent.setJobExecutionId(jobExecutionId);
        ent.setStartTime(startTime);
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);
        ent.setJobParameters(studentSearchRequest);

        return gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
    }

    protected void updateBatchJobHistory(BatchGradAlgorithmJobHistoryEntity entity, Long readCount) {
        entity.setExpectedStudentsProcessed(readCount);
        gradBatchHistoryService.saveGradAlgorithmJobHistory(entity);
    }
}
