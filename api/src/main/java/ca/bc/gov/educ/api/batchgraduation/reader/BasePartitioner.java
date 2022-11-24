package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.RunTypeEnum;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class BasePartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePartitioner.class);
    private static final String RERUN_TYPE = "reRunType";
    private static final String RUN_BY = "runBy";
    private static final String PREV_BATCH_ID = "previousBatchId";
    private static final String RERUN_ALL = "RERUN_ALL";
    private static final String RERUN_FAILED = "RERUN_FAILED";

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    protected RunTypeEnum runType;

    protected abstract JobExecution getJobExecution();

    protected void initializeRunType() {
        JobParameters jobParameters = getJobExecution().getJobParameters();
        String runTypeStr = jobParameters.getString(RERUN_TYPE);
        if (StringUtils.isBlank(runTypeStr)) {
            runType = RunTypeEnum.NORMAL_JOB_PROCESS;
        } else if (StringUtils.equals(RERUN_ALL, runTypeStr)) {
            runType = RunTypeEnum.RERUN_ALL_STUDENTS_FROM_PREVIOUS_JOB;
        } else if (StringUtils.equals(RERUN_FAILED, runTypeStr)) {
            runType = RunTypeEnum.RERUN_ERRORED_STUDENTS_FROM_PREVIOUS_JOB;
        } else {
            runType = RunTypeEnum.NORMAL_JOB_PROCESS;
        }
    }

    protected void createTotalSummaryDTO(String summaryContextName) {
        AlgorithmSummaryDTO totalSummaryDTO = (AlgorithmSummaryDTO)getJobExecution().getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new AlgorithmSummaryDTO();
            getJobExecution().getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
    }

    protected List<UUID> getInputDataFromPreviousJob() {
        Long batchId = getJobExecution().getId();
        JobParameters jobParameters = getJobExecution().getJobParameters();
        Long fromBatchId = jobParameters.getLong(PREV_BATCH_ID);
        String username = jobParameters.getString(RUN_BY);
        if (runType == RunTypeEnum.RERUN_ALL_STUDENTS_FROM_PREVIOUS_JOB) {
            copyAllStudentsFromPreviousJob(batchId, fromBatchId, username);
            return getInputDataForAllStudents(batchId);
        } else {
            copyErroredStudentsFromPreviousJob(batchId, fromBatchId, username);
            return getInputDataForErroredStudents(batchId);
        }
    }

    protected List<UUID> getInputDataForErroredStudents(Long batchId) {
        List<BatchGradAlgorithmStudentEntity> entityList = gradBatchHistoryService.getErroredStudents(batchId);
        if (entityList != null && !entityList.isEmpty()) {
            return entityList.stream().map(BatchGradAlgorithmStudentEntity::getStudentID).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    protected List<UUID> getInputDataForAllStudents(Long batchId) {
        List<BatchGradAlgorithmStudentEntity> entityList = gradBatchHistoryService.getAllStudents(batchId);
        if (entityList != null && !entityList.isEmpty()) {
            return entityList.stream().map(BatchGradAlgorithmStudentEntity::getStudentID).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    protected void saveInputData(List<UUID> studentIDs) {
        Long jobExecutionId = getJobExecution().getId();
        long startTime = System.currentTimeMillis();
        LOGGER.info(" => Saving Input Data for {} students", studentIDs.size());

        List<BatchGradAlgorithmStudentEntity> entityList = new ArrayList<>();
        studentIDs.forEach(id -> {
            BatchGradAlgorithmStudentEntity ent = new BatchGradAlgorithmStudentEntity();
            ent.setJobExecutionId(jobExecutionId);
            ent.setStudentID(id);
            ent.setStatus(BatchStatusEnum.STARTED.name());
            entityList.add(ent);
        });
        gradBatchHistoryService.saveGradAlgorithmStudents(entityList);
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        LOGGER.info(" => Saving Input Data is completed in {} secs", diff);
    }

    protected BatchGradAlgorithmJobHistoryEntity createBatchJobHistory() {
        Long jobExecutionId = getJobExecution().getId();
        JobParameters jobParameters = getJobExecution().getJobParameters();
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");
        String username = jobParameters.getString(RUN_BY);
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
        ent.setCreateUser(username);
        ent.setUpdateUser(username);

        return gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
    }

    protected void updateBatchJobHistory(BatchGradAlgorithmJobHistoryEntity entity, Long readCount) {
        entity.setExpectedStudentsProcessed(readCount);
        gradBatchHistoryService.saveGradAlgorithmJobHistory(entity);
    }

    private void copyAllStudentsFromPreviousJob(Long batchId, Long fromBatchId, String username) {
        gradBatchHistoryService.copyAllStudentsIntoNewBatch(batchId, fromBatchId, username);
    }

    private void copyErroredStudentsFromPreviousJob(Long batchId, Long fromBatchId, String username) {
        gradBatchHistoryService.copyErroredStudentsIntoNewBatch(batchId, fromBatchId, username);
    }
}
