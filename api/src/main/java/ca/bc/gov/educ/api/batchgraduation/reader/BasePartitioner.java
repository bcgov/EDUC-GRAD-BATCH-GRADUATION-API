package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.transform.TransformerException;
import java.util.*;

public abstract class BasePartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePartitioner.class);
    private static final String RERUN_TYPE = "reRunType";
    private static final String RUN_BY = "runBy";
    private static final String PREV_BATCH_ID = "previousBatchId";
    private static final String RERUN_ALL = "RERUN_ALL";
    private static final String RERUN_FAILED = "RERUN_FAILED";

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    JsonTransformer jsonTransformer;

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
            return entityList.stream().map(BatchGradAlgorithmStudentEntity::getStudentID).toList();
        }
        return new ArrayList<>();
    }

    protected List<UUID> getInputDataForAllStudents(Long batchId) {
        List<BatchGradAlgorithmStudentEntity> entityList = gradBatchHistoryService.getAllStudents(batchId);
        if (entityList != null && !entityList.isEmpty()) {
            return entityList.stream().map(BatchGradAlgorithmStudentEntity::getStudentID).toList();
        }
        return new ArrayList<>();
    }

    protected void saveInputData(List<UUID> studentIDs) {
        Long jobExecutionId = getJobExecution().getId();
        JobParameters jobParameters = getJobExecution().getJobParameters();
        String username = jobParameters.getString(RUN_BY);
        long startTime = System.currentTimeMillis();
        LOGGER.info(" => Saving Input Data for {} students", studentIDs.size());

        List<BatchGradAlgorithmStudentEntity> entityList = new ArrayList<>();
        studentIDs.forEach(id -> {
            BatchGradAlgorithmStudentEntity ent = new BatchGradAlgorithmStudentEntity();
            ent.setJobExecutionId(jobExecutionId);
            ent.setStudentID(id);
            ent.setStatus(BatchStatusEnum.STARTED.name());
            ent.setCreateUser(username);
            ent.setUpdateUser(username);
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

    Map<String, ExecutionContext> getStringExecutionContextMap(int gridSize, List<StudentCredentialDistribution> credentialList, String credentialType, Logger logger) {
        StudentSearchRequest request = getStudentSearchRequest();
        Iterator scdIt = credentialList.stream().iterator();
        while (scdIt.hasNext()) {
            StudentCredentialDistribution scd = (StudentCredentialDistribution)scdIt.next();
            String districtCode = StringUtils.substring(scd.getSchoolOfRecord(), 0, 3);
            if (
                    (request.getDistricts() != null && !request.getDistricts().isEmpty() && !request.getDistricts().contains(districtCode))
                    ||
                    (request.getSchoolOfRecords() != null && !request.getSchoolOfRecords().isEmpty() && !request.getSchoolOfRecords().contains(scd.getSchoolOfRecord()))
            ) {
                scdIt.remove();
            }
        }
        sortStudentCredentialDistributionByNames(credentialList);
        int partitionSize = credentialList.size()/gridSize + 1;
        List<List<StudentCredentialDistribution>> partitions = new LinkedList<>();
        for (int i = 0; i < credentialList.size(); i += partitionSize) {
            partitions.add(credentialList.subList(i, Math.min(i + partitionSize, credentialList.size())));
        }
        Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
        for (int i = 0; i < partitions.size(); i++) {
            ExecutionContext executionContext = new ExecutionContext();
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.initializeCredentialCountMap();
            List<StudentCredentialDistribution> data = partitions.get(i);
            executionContext.put("data", data);
            summaryDTO.setReadCount(data.size());
            if(credentialType != null){
                summaryDTO.setCredentialType(credentialType);
            }
            executionContext.put("summary", summaryDTO);
            executionContext.put("index",0);
            String key = "partition" + i;
            map.put(key, executionContext);
        }
        logger.info("Found {} in total running on {} partitions",credentialList.size(),map.size());
        return map;
    }

    protected StudentSearchRequest getStudentSearchRequest() {
        JobParameters jobParameters = getJobExecution().getJobParameters();
        StudentSearchRequest request;
        try {
            request = (StudentSearchRequest)jsonTransformer.unmarshall(jobParameters.getString("BATCH_REQUEST", "{}"), StudentSearchRequest.class);
        } catch (TransformerException e) {
            LOGGER.warn("Unable to deserialize YearEndBatchJobRequest object");
            request = new StudentSearchRequest();
        }
        return request;
    }

    static void sortStudentCredentialDistributionByNames(List<StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalMiddleNames, Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
