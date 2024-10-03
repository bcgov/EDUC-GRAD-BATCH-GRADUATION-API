package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ProcessError;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;
import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.USER_SCHEDULED;

public abstract class BaseRunCompletionNotificationListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseRunCompletionNotificationListener.class);

    @Autowired
    private GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Autowired
    private RestUtils restUtils;

    @Autowired
    JsonTransformer jsonTransformer;

    protected void handleSummary(JobExecution jobExecution, String summaryDtoName, boolean isSpecialRun) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        Long jobExecutionId = jobExecution.getId();
        String status = jobExecution.getStatus().toString();
        Date startTime = DateUtils.toDate(jobExecution.getStartTime());
        Date endTime = DateUtils.toDate(jobExecution.getEndTime());
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");

        String searchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
        StudentSearchRequest req = (StudentSearchRequest)jsonTransformer.unmarshall(searchRequest, StudentSearchRequest.class);

        AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get(summaryDtoName);
        if(summaryDTO == null) {
            summaryDTO = new AlgorithmSummaryDTO();
        }

        if (isSpecialRun) {
            String userScheduledId = jobParameters.getString(USER_SCHEDULED);
            if (userScheduledId != null) {
                taskSchedulingService.updateUserScheduledJobs(userScheduledId);
            }
        }

        // retrieve counts from database
        summaryDTO.setReadCount(getTotalReadCount(jobExecutionId));
        summaryDTO.setProcessedCount(getTotalProcessedCount(jobExecutionId));
        summaryDTO.setErroredCount(getTotalErroredCount(jobExecutionId));

        // display Summary Details
        LOGGER.info("Records read   : {}", summaryDTO.getReadCount());
        LOGGER.info("Processed count: {}", summaryDTO.getProcessedCount());
        LOGGER.info(" --------------------------------------------------------------------------------------");
        LOGGER.info("Errors:{}", summaryDTO.getErroredCount());

        // save batch job & error history
        processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime);
        LOGGER.info(" --------------------------------------------------------------------------------------");
        Map<String, Integer> gradCountMap = gradBatchHistoryService.getGraduationProgramCountsForBatchRunSummary(jobExecutionId);
        if (gradCountMap != null && !gradCountMap.isEmpty()) {
            gradCountMap.keySet().stream().forEach(k -> LOGGER.info(" {} count   : {}", k, gradCountMap.get(k)));
        }
        LOGGER.info(" --------------------------------------------------------------------------------------");

        ResponseObj obj = restUtils.getTokenResponseObject();
        if (!isSpecialRun) {
            updateBackStudentFlagForErroredStudents(summaryDTO.getErrors(), jobType);
        }
        processSchoolList(jobExecutionId, jobType);
    }

    private void processBatchJobHistory(AlgorithmSummaryDTO summaryDTO, Long jobExecutionId, String status, String jobTrigger, String jobType, Date startTime, Date endTime) {
        Long failedRecords = summaryDTO.getErroredCount();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords != null? failedRecords.intValue() : 0);
        ent.setJobExecutionId(jobExecutionId);
        ent.setStartTime(DateUtils.toLocalDateTime(startTime));
        ent.setEndTime(DateUtils.toLocalDateTime(endTime));
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);

        gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
    }

    private void updateBackStudentFlagForErroredStudents(Map<UUID, ProcessError> errors, String jobType) {
        List<UUID> erroredStudentIDs = new ArrayList<>(errors.keySet());
        if (!erroredStudentIDs.isEmpty()) {
            LOGGER.info(" Update Student Flags: [{}] for {} errored students ----------------------------", jobType, erroredStudentIDs.size());
            String result = restUtils.updateStudentFlagReadyForBatch(erroredStudentIDs, jobType);
            LOGGER.info(" Update Student Flags completed {} ----------------------------", result);
        }
    }

    private void processSchoolList(Long batchId, String jobType) {
        LOGGER.info(" Creating Reports for {}", jobType);
        List<String> uniqueSchoolList = gradBatchHistoryService.getSchoolListForReport(batchId);
        LOGGER.info(" Number of Schools [{}]", uniqueSchoolList.size());
        restUtils.createAndStoreSchoolReports(uniqueSchoolList,jobType);
    }

    private long getTotalReadCount(Long batchId) {
        return gradBatchHistoryService.getCountForReadStudent(batchId);
    }

    private long getTotalProcessedCount(Long batchId) {
        return gradBatchHistoryService.getCountForProcessedStudent(batchId);
    }

    private long getTotalErroredCount(Long batchId) {
        return gradBatchHistoryService.getCountForErroredStudent(batchId);
    }

}
