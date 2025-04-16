package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import ca.bc.gov.educ.api.batchgraduation.util.GradSorter;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

public abstract class BasePartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePartitioner.class);
    private static final String RERUN_TYPE = "reRunType";
    protected static final String RUN_BY = "runBy";
    private static final String PREV_BATCH_ID = "previousBatchId";
    private static final String RERUN_ALL = "RERUN_ALL";
    private static final String RERUN_FAILED = "RERUN_FAILED";

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    RestUtils restUtils;

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
        return createBatchJobHistory(jobExecutionId, getJobExecution(), 0);
    }

    protected BatchGradAlgorithmJobHistoryEntity createBatchJobHistory(Long jobExecutionId, JobExecution jobExecution, long expectedStudentsProcessed) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");
        String username = jobParameters.getString(RUN_BY);
        String studentSearchRequest = jobParameters.getString(SEARCH_REQUEST);
        String status = jobExecution.getStatus().toString();
        Date startTime = DateUtils.toDate(jobExecution.getStartTime());

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(0L);
        ent.setExpectedStudentsProcessed(expectedStudentsProcessed);
        ent.setFailedStudentsProcessed(0);
        ent.setJobExecutionId(jobExecutionId);
        ent.setStartTime(DateUtils.toLocalDateTime(startTime));
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

    void filterStudentCredentialDistribution(List<? extends StudentCredentialDistribution> credentialList) {
        LOGGER.debug("Filter Student Credential Distribution for {} student credentials", credentialList.size());
        StudentSearchRequest request = getStudentSearchRequest();
        Iterator scdIt = credentialList.iterator();
        while (scdIt.hasNext()) {
            StudentCredentialDistribution scd = (StudentCredentialDistribution)scdIt.next();
            if (
                    (scd.getDistrictId() != null && request.getDistrictIds() != null && !request.getDistrictIds().isEmpty() && !request.getDistrictIds().contains(scd.getDistrictId()))
                    ||
                    (scd.getSchoolId() != null && request.getSchoolIds() != null && !request.getSchoolIds().isEmpty() && !request.getSchoolIds().contains(scd.getSchoolId()))
            ) {
                scdIt.remove();
                LOGGER.debug("Student Credential {}/{} removed by the filter \"{}\"", scd.getStudentID(), scd.getSchoolId(), String.join(",", request.getDistrictIds().toString()));
            }
        }
        LOGGER.debug("Total {} Student Credentials selected after filter", credentialList.size());
    }

    Map<String, ExecutionContext> getStringExecutionContextMap(int gridSize, List<? extends StudentCredentialDistribution> credentialList, String credentialType) {
        filterStudentCredentialDistribution(credentialList);
        sortStudentCredentialDistributionByNames(credentialList);
        int partitionSize = credentialList.size()/gridSize + 1;
        List<List<? extends StudentCredentialDistribution>> partitions = new LinkedList<>();
        for (int i = 0; i < credentialList.size(); i += partitionSize) {
            partitions.add(credentialList.subList(i, Math.min(i + partitionSize, credentialList.size())));
        }
        Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
        for (int i = 0; i < partitions.size(); i++) {
            ExecutionContext executionContext = new ExecutionContext();
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.initializeCredentialCountMap();
            List<? extends StudentCredentialDistribution> data = partitions.get(i);
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
        LOGGER.info("Found {} in total running on {} partitions",credentialList.size(),map.size());
        return map;
    }

    StudentSearchRequest getStudentSearchRequest() {
        JobParameters jobParameters = getJobExecution().getJobParameters();
        return (StudentSearchRequest)jsonTransformer.unmarshall(jobParameters.getString(SEARCH_REQUEST, "{}"), StudentSearchRequest.class);
    }

    void sortStudentCredentialDistributionByNames(List<? extends StudentCredentialDistribution> students) {
        GradSorter.sortStudentCredentialDistributionByNames(students);
    }

    void filterByStudentSearchRequest(List<? extends StudentCredentialDistribution> eligibleStudentSchoolDistricts) {
        StudentSearchRequest searchRequest = getStudentSearchRequest();
        if(searchRequest != null && searchRequest.getSchoolCategoryCodes() != null && !searchRequest.getSchoolCategoryCodes().isEmpty()) {
            List<UUID> useFilterSchoolDistricts = new ArrayList<>();
            for(String schoolCategoryCode: searchRequest.getSchoolCategoryCodes()) {
                LOGGER.debug("Use schoolCategory code {} to find list of schools", schoolCategoryCode);
                List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> schools = restUtils.getSchoolsBySchoolCategoryCode(schoolCategoryCode);
                for(ca.bc.gov.educ.api.batchgraduation.model.institute.School school: schools) {
                    LOGGER.debug("SchoolId {} / Mincode {} found by schoolCategory code {}", school.getSchoolId(), school.getMincode(), schoolCategoryCode);
                    useFilterSchoolDistricts.add(UUID.fromString(school.getSchoolId()));
                }
            }
            eligibleStudentSchoolDistricts.removeIf(scr->scr.getSchoolId() != null && !useFilterSchoolDistricts.contains(scr.getSchoolId()));
            LOGGER.debug("Student Credential Distribution filtered by schoolCategory code {}: {}", searchRequest.getSchoolCategoryCodes(), eligibleStudentSchoolDistricts.size());
        }
        if(searchRequest != null && searchRequest.getDistrictIds() != null && !searchRequest.getDistrictIds().isEmpty()) {
            eligibleStudentSchoolDistricts.removeIf(scr->scr.getDistrictId() != null && !searchRequest.getDistrictIds().contains(scr.getDistrictId()));
            LOGGER.debug("Student Credential Distribution filtered by district id {}: {}", searchRequest.getDistrictIds(), eligibleStudentSchoolDistricts.size());
        }
        if(searchRequest != null && searchRequest.getSchoolIds() != null && !searchRequest.getSchoolIds().isEmpty()) {
            eligibleStudentSchoolDistricts.removeIf(scr->scr.getSchoolId() != null && !searchRequest.getSchoolIds().contains(scr.getSchoolId()));
            LOGGER.debug("Student Credential Distribution filtered by school id {}: {}", searchRequest.getSchoolIds(), eligibleStudentSchoolDistricts.size());
        }
        if(searchRequest != null && searchRequest.getStudentIDs() != null && !searchRequest.getStudentIDs().isEmpty()) {
            eligibleStudentSchoolDistricts.removeIf(scr->scr.getStudentID() != null && !searchRequest.getStudentIDs().contains(scr.getStudentID()));
            LOGGER.debug("Student Credential Distribution filtered by student ID {}: {}", searchRequest.getStudentIDs(), eligibleStudentSchoolDistricts.size());
        }
        if(searchRequest != null && searchRequest.getPens() != null && !searchRequest.getPens().isEmpty()) {
            eligibleStudentSchoolDistricts.removeIf(scr->StringUtils.isNotBlank(scr.getPen()) && !searchRequest.getPens().contains(scr.getPen()));
            LOGGER.debug("Student Credential Distribution filtered by pen {}: {}", searchRequest.getPens(), eligibleStudentSchoolDistricts.size());
        }
    }

    void filterOutDeceasedStudents(List<StudentCredentialDistribution> credentialList) {
        LOGGER.debug("Total size of credential list: {}", credentialList.size());
        List<UUID> deceasedIDs = restUtils.getDeceasedStudentIDs(credentialList.stream().map(StudentCredentialDistribution::getStudentID).distinct().toList());
        if (!deceasedIDs.isEmpty()) {
            LOGGER.debug("Deceased students: {}", deceasedIDs.size());
            credentialList.removeIf(cr -> deceasedIDs.contains(cr.getStudentID()));
            LOGGER.debug("Revised size of credential list: {}", credentialList.size());
        }
    }
}
