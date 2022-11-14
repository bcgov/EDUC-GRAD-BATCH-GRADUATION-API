package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ProcessError;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public abstract class BaseRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseRunCompletionNotificationListener.class);

    @Autowired
    private GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Autowired
    private RestUtils restUtils;

    protected void handleSummary(JobExecution jobExecution, String summaryDtoName, boolean isSpecialRun) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        Long jobExecutionId = jobExecution.getId();
        String status = jobExecution.getStatus().toString();
        Date startTime = jobExecution.getStartTime();
        Date endTime = jobExecution.getEndTime();
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");

        AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get(summaryDtoName);
        if(summaryDTO == null) {
            summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.initializeProgramCountMap();
        }

        if (isSpecialRun) {
            String userScheduledId = jobParameters.getString("userScheduled");
            if (userScheduledId != null) {
                taskSchedulingService.updateUserScheduledJobs(userScheduledId);
            }
        } else {
            for (UUID successfulStudentID : summaryDTO.getSuccessfulStudentIDs()) {
                summaryDTO.getErrors().keySet().removeIf(t -> t.compareTo(successfulStudentID)==0);
            }
        }

        // display Summary Details
        LOGGER.info("Records read   : {}", summaryDTO.getTotalReadCount());
        LOGGER.info("Processed count: {}", summaryDTO.getProcessedCount());
        LOGGER.info(" --------------------------------------------------------------------------------------");
        LOGGER.info("Errors:{}", summaryDTO.getErrors().size());

        // save batch job & error history
        processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime);
        LOGGER.info(" --------------------------------------------------------------------------------------");
        AlgorithmSummaryDTO finalSummaryDTO = summaryDTO;
        summaryDTO.getProgramCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getProgramCountMap().get(key)));

        ResponseObj obj = restUtils.getTokenResponseObject();
        if (!isSpecialRun) {
            updateBackStudentFlagForErroredStudents(summaryDTO.getErrors(), jobType, obj.getAccess_token());
        }
        processSchoolList(summaryDTO.getSchoolList(), obj.getAccess_token(), jobType);
    }

    private void processBatchJobHistory(AlgorithmSummaryDTO summaryDTO, Long jobExecutionId, String status, String jobTrigger, String jobType, Date startTime, Date endTime) {
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(jobExecutionId);
        ent.setStartTime(startTime);
        ent.setEndTime(endTime);
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);

        gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);

        List<BatchGradAlgorithmErrorHistoryEntity> eList = new ArrayList<>();
        summaryDTO.getErrors().forEach((e,v) -> {
            LOGGER.info(" Student ID : {}, Reason: {}, Detail: {}", e, v.getReason(), v.getDetail());
            BatchGradAlgorithmErrorHistoryEntity errorHistory = new BatchGradAlgorithmErrorHistoryEntity();
            errorHistory.setStudentID(e);
            errorHistory.setJobExecutionId(jobExecutionId);
            errorHistory.setError(v.getReason() + "-" + v.getDetail());
            eList.add(errorHistory);
        });
        if(!eList.isEmpty()) {
            gradBatchHistoryService.saveGradAlgorithmErrorHistories(eList);
        }
    }

    private void updateBackStudentFlagForErroredStudents(Map<UUID, ProcessError> errors, String jobType, String accessToken) {
        List<UUID> erroredStudentIDs = new ArrayList<>(errors.keySet());
        if (!erroredStudentIDs.isEmpty()) {
            LOGGER.info(" Update Student Flags: [{}] for {} errored students ----------------------------", jobType, erroredStudentIDs.size());
            restUtils.updateStudentFlagReadyForBatch(erroredStudentIDs, jobType, accessToken);
        }
    }

    private void processSchoolList(Set<String> cList, String accessToken, String jobType) {
        LOGGER.info(" Creating Reports for {} --------------------------------------------------------", jobType);
        List<String> uniqueSchoolList = new ArrayList<>(cList);
        LOGGER.info(" Number of Schools [{}] ---------------------------------------------------------", uniqueSchoolList.size());
        restUtils.createAndStoreSchoolReports(accessToken,uniqueSchoolList,jobType);
    }

}
