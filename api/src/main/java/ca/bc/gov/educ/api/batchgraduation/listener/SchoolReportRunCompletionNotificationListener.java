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
public class SchoolReportRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolReportRunCompletionNotificationListener.class);
    
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
	    	LOGGER.info("School Report Posting  Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			SchoolReportSummaryDTO summaryDTO = (SchoolReportSummaryDTO)jobContext.get("schoolReportSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new SchoolReportSummaryDTO();
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
			SchoolReportSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key));
			});
			LOGGER.info("=======================================================================================");
		}
    }

	private void processGlobalList(List<SchoolReportDistribution> cList, Long batchId, Map<String,DistributionPrintRequest> mapDist,String accessToken) {
		List<String> uniqueSchoolList = cList.stream().map(SchoolReportDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		List<SchoolReportDistribution> finalCList = cList;
		uniqueSchoolList.forEach(usl->{
			SchoolReportDistribution gradReport = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getReportTypeCode().compareTo("GRAD") == 0).findAny().orElse(null);
			SchoolReportDistribution nongradReport = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getReportTypeCode().compareTo("NONGRAD") == 0).findAny().orElse(null);
			SchoolReportDistribution nongradprjReport = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getReportTypeCode().compareTo("NONGRADPRJ") == 0).findAny().orElse(null);
			schoolReportPrintFile(gradReport,nongradReport,nongradprjReport,batchId,usl,mapDist);
		});
		DistributionResponse disres = restUtils.readAndPostSchoolReports(batchId,accessToken,mapDist);
		if(disres != null) {
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(cList,obj.getAccess_token());
		}
	}

	private void updateBackStudentRecords(List<SchoolReportDistribution> cList,String accessToken) {
		cList.forEach(scd-> {
			restUtils.updateSchoolReportRecord(scd.getSchoolOfRecord(),scd.getReportTypeCode(),accessToken);
		});
	}

	private void schoolReportPrintFile(SchoolReportDistribution gradReport, SchoolReportDistribution nongradReport, SchoolReportDistribution nongradprjReport, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		SchoolReportPostRequest schReq = new SchoolReportPostRequest();
		schReq.setBatchId(batchId);
		schReq.setPsId(usl +" " +batchId);
		schReq.setGradReport(gradReport);
		schReq.setNongradReport(nongradReport);
		schReq.setNongradprjreport(nongradprjReport);

		if(mapDist.get(usl) != null) {
			DistributionPrintRequest dist = mapDist.get(usl);
			dist.setSchoolReportPostRequest(schReq);
			dist.setTotal(dist.getTotal()+1);
			mapDist.put(usl,dist);
		}else{
			DistributionPrintRequest dist = new DistributionPrintRequest();
			dist.setSchoolReportPostRequest(schReq);
			dist.setTotal(dist.getTotal()+1);
			mapDist.put(usl,dist);
		}
	}
}
