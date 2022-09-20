package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.controller.JobLauncherController;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TvrRunJobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvrRunJobCompletionNotificationListener.class);

	@Autowired
	private GradBatchHistoryService gradBatchHistoryService;

	@Autowired
	JobLauncherController jobLauncherController;
    
    @Autowired
    private RestUtils restUtils;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("TVR Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			
			AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("tvrRunSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new AlgorithmSummaryDTO();
			}
			for (UUID successfulStudentID : summaryDTO.getSuccessfulStudentIDs()) {
				summaryDTO.getErrors().keySet().removeIf(t -> t.compareTo(successfulStudentID)==0);
			}

			int failedRecords = summaryDTO.getErrors().size();			
			Long processedStudents = summaryDTO.getProcessedCount();
			Long expectedStudents = summaryDTO.getReadCount();
			ResponseObj obj = restUtils.getTokenResponseObject();
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

			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Errors:{}", summaryDTO.getErrors().size());
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
			LOGGER.info(" --------------------------------------------------------------------------------------");
			AlgorithmSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getProgramCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getProgramCountMap().get(key)));


			LOGGER.info(" Creating Reports ---------------------------------------------------------------------");
			processGlobalList(summaryDTO.getGlobalList(),obj.getAccess_token());
			LOGGER.info("=======================================================================================");
		}
    }

	private void processGlobalList(List<GraduationStudentRecord> cList, String accessToken) {
		List<String> uniqueSchoolList = cList.stream().map(GraduationStudentRecord::getSchoolOfRecord).distinct().collect(Collectors.toList());
		restUtils.createAndStoreSchoolReports(accessToken,uniqueSchoolList,"TVRRUN");
	}
}
