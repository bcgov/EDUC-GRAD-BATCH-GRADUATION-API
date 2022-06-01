package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
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
	    	LOGGER.info("TVR Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());
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
			int failedRecords = summaryDTO.getErrors().size();			
			Long processedStudents = summaryDTO.getProcessedCount();
			Long expectedStudents = summaryDTO.getReadCount();
			ResponseObj obj = restUtils.getTokenResponseObject();
			processGlobalList(summaryDTO.getGlobalList(),summaryDTO.getMapDist(),jobExecutionId,obj.getAccess_token());
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
	    	LOGGER.info("TVR Job failed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			
			AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("tvrRunSummaryDTO");
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

	private void processGlobalList(List<GraduationStudentRecord> cList, Map<String, SchoolReportRequest> mapDist, Long batchId, String accessToken) {
		List<String> uniqueSchoolList = cList.stream().map(GraduationStudentRecord::getSchoolOfRecord).distinct().collect(Collectors.toList());
		List<GraduationStudentRecord> finalCList = cList;
		uniqueSchoolList.forEach(usl->{
			List<GraduationStudentRecord> stdList = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0).collect(Collectors.toList());
			schoolReportRequest(stdList,usl,mapDist);
		});
		restUtils.createAndStoreSchoolReports(accessToken,mapDist);
	}

	private void schoolReportRequest(List<GraduationStudentRecord> studentList, String usl, Map<String,SchoolReportRequest> mapDist) {
		if(!studentList.isEmpty()) {
			if(mapDist.get(usl) != null) {
				SchoolReportRequest srr = mapDist.get(usl);
				srr.setStudentList(studentList);
				mapDist.put(usl,srr);
			}else{
				SchoolReportRequest srr = new SchoolReportRequest();
				srr.setStudentList(studentList);
				mapDist.put(usl,srr);
			}
		}
	}
}
