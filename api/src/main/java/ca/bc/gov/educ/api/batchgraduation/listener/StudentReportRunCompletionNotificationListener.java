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
public class StudentReportRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentReportRunCompletionNotificationListener.class);
    
    @Autowired BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;
	@Autowired BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;
    @Autowired RestUtils restUtils;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Student Report Posting Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			StudentReportSummaryDTO summaryDTO = (StudentReportSummaryDTO)jobContext.get("studentReportSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new StudentReportSummaryDTO();
			}
			int failedRecords = summaryDTO.getErrors().size();			
			Long processedStudents = summaryDTO.getProcessedCount();
			Long expectedStudents = summaryDTO.getReadCount();
			ResponseObj obj = restUtils.getTokenResponseObject();
			processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),obj.getAccess_token());
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
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
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
			StudentReportSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));
			LOGGER.info("=======================================================================================");
		}
    }

	private void processGlobalList(List<SchoolStudentCredentialDistribution> cList, Long batchId, Map<String,DistributionPrintRequest> mapDist,String accessToken) {
		List<String> uniqueSchoolList = cList.stream().map(SchoolStudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		uniqueSchoolList.forEach(usl->{
			List<SchoolStudentCredentialDistribution> transcriptList = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getCredentialTypeCode().compareTo("ACHV") == 0).collect(Collectors.toList());
			List<SchoolStudentCredentialDistribution> tvrList = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getCredentialTypeCode().compareTo("ACHV") != 0).collect(Collectors.toList());
			transcriptPrintPostingFile(transcriptList,batchId,usl,mapDist);
			achievementPrintPostingFile(tvrList,batchId,usl,mapDist);
		});
		DistributionResponse disres = restUtils.readAndPostStudentReports(batchId,accessToken,mapDist);
		if(disres != null) {
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(cList,obj.getAccess_token());
		}
	}

	private void updateBackStudentRecords(List<SchoolStudentCredentialDistribution> cList,String accessToken) {
		cList.forEach(scd-> restUtils.updateStudentCredentialRecordPosting(scd.getStudentID(),scd.getCredentialTypeCode(),accessToken));
	}

	private void transcriptPrintPostingFile(List<SchoolStudentCredentialDistribution> yed4List, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		if(!yed4List.isEmpty()) {
			TranscriptPrintPostingRequest tpReq = new TranscriptPrintPostingRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(yed4List.size());
			tpReq.setTranscriptList(yed4List);
			if(mapDist.get(usl) != null) {
				DistributionPrintRequest dist = mapDist.get(usl);
				dist.setTranscriptPrintPostingRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}else{
				DistributionPrintRequest dist = new DistributionPrintRequest();
				dist.setTranscriptPrintPostingRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}
		}
	}

	private void achievementPrintPostingFile(List<SchoolStudentCredentialDistribution> cList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		if(!cList.isEmpty()) {
			TVRReportPrintPostingRequest tpReq = new TVRReportPrintPostingRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(cList.size());
			tpReq.setTvrList(cList);
			if(mapDist.get(usl) != null) {
				DistributionPrintRequest dist = mapDist.get(usl);
				dist.setTvrReportPrintPostingRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}else{
				DistributionPrintRequest dist = new DistributionPrintRequest();
				dist.setTvrReportPrintPostingRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}
		}
	}
}
