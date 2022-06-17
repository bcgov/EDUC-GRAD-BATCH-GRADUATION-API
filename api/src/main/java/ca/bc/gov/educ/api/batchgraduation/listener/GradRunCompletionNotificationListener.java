package ca.bc.gov.educ.api.batchgraduation.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchGradAlgorithmErrorHistory;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;

@Component
public class GradRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradRunCompletionNotificationListener.class);
    
    @Autowired
    private BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

	@Autowired
	private BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;
    
    @Autowired
    private RestUtils restUtils;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Grad Algorithm Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			
			AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("regGradAlgSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new AlgorithmSummaryDTO();
			}
			for (UUID successfulStudentID : summaryDTO.getSuccessfulStudentIDs()) {
				summaryDTO.getErrors().removeIf(t -> t.getStudentID().equalsIgnoreCase(successfulStudentID.toString()));
			}

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

			batchGradAlgorithmJobHistoryRepository.save(ent);
			
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Errors:		   {}", summaryDTO.getErrors().size());
			List<BatchGradAlgorithmErrorHistoryEntity> eList = new ArrayList<>();
			summaryDTO.getErrors().forEach(e -> {
				LOGGER.info(" Student ID : {}, Reason: {}, Detail: {}", e.getStudentID(), e.getReason(), e.getDetail());
				BatchGradAlgorithmErrorHistoryEntity errorHistory = new BatchGradAlgorithmErrorHistoryEntity();
				errorHistory.setStudentID(UUID.fromString(e.getStudentID()));
				errorHistory.setJobExecutionId(jobExecutionId);
				errorHistory.setError(e.getReason() + "-" + e.getDetail());
				eList.add(errorHistory);
			});
			if(!eList.isEmpty())
				batchGradAlgorithmErrorHistoryRepository.saveAll(eList);

			LOGGER.info(" --------------------------------------------------------------------------------------");
			AlgorithmSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getProgramCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getProgramCountMap().get(key));
			});
			LOGGER.info("=======================================================================================");
		}else if (jobExecution.getStatus() == BatchStatus.FAILED) {
			long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Grad Algorithm Job failed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			
			AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("regGradAlgSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new AlgorithmSummaryDTO();
			}
			int failedRecords = 0;			
			List<GraduationStudentRecord> list = restUtils.getStudentsForAlgorithm(summaryDTO.getAccessToken());
			if(!list.isEmpty()) {
				failedRecords = list.size();
			}	
			
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
			
			batchGradAlgorithmJobHistoryRepository.save(ent);
			
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Errors		 : {}", summaryDTO.getErrors().size());
			List<BatchGradAlgorithmErrorHistoryEntity> eList = new ArrayList<>();
			summaryDTO.getErrors().forEach(e -> {
				LOGGER.info(" Student ID : {}, Reason: {}, Detail: {}", e.getStudentID(), e.getReason(), e.getDetail());
				BatchGradAlgorithmErrorHistoryEntity errorHistory = new BatchGradAlgorithmErrorHistoryEntity();
				errorHistory.setStudentID(UUID.fromString(e.getStudentID()));
				errorHistory.setJobExecutionId(jobExecutionId);
				errorHistory.setError(e.getReason() + "-" + e.getDetail());
				eList.add(errorHistory);
			});

			if(!eList.isEmpty())
				batchGradAlgorithmErrorHistoryRepository.saveAll(eList);
			LOGGER.info(" --------------------------------------------------------------------------------------");
			AlgorithmSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getProgramCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getProgramCountMap().get(key));
			});
			LOGGER.info("=======================================================================================");
		}
    }
}
