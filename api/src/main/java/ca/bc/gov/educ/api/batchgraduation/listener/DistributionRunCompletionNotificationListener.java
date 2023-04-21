package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class DistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunCompletionNotificationListener.class);

	@Autowired
	SupportListener supportListener;

    @Override
    public void afterJob(JobExecution jobExecution) {
		long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
		LOGGER.info("=======================================================================================");
		LOGGER.info("Distribution Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
		JobParameters jobParameters = jobExecution.getJobParameters();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		Long jobExecutionId = jobExecution.getId();
		String status = jobExecution.getStatus().toString();
		Date startTime = jobExecution.getStartTime();
		Date endTime = jobExecution.getEndTime();
		String jobTrigger = jobParameters.getString("jobTrigger");
		String jobType = jobParameters.getString("jobType");
		String activityCode = "MONTHLYDIST";
		if(StringUtils.isNotBlank(jobType)) {
			switch (jobType) {
				case "DISTRUN" -> activityCode = "MONTHLYDIST";
				case "DISTRUN_YE" -> activityCode = "YEARENDDIST";
				case "DISTRUN_SUPP" -> activityCode = "SUPPDIST";
				case "NONGRADRUN" -> activityCode = "NONGRADDIST";
			}
		}
		String studentSearchRequest = jobParameters.getString("searchRequest");

		DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
		if(summaryDTO == null) {
			summaryDTO = new DistributionSummaryDTO();
			summaryDTO.initializeCredentialCountMap();
		}

		// display Summary Details
		LOGGER.info("Records read   : {}", summaryDTO.getReadCount());
		LOGGER.info("Processed count: {}", summaryDTO.getProcessedCount());
		LOGGER.info(" --------------------------------------------------------------------------------------");
		LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
		LOGGER.info(" --------------------------------------------------------------------------------------");
		DistributionSummaryDTO finalSummaryDTO = summaryDTO;
		summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

		ResponseObj tokenResponse = restUtils.getTokenResponseObject();
		LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
		//TODO: catch errors here, break up processGlobalList.
		DistributionResponse distributionResponse = null;
		try {
			distributionResponse = processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),activityCode,tokenResponse.getAccess_token());
		} catch (Exception e) {
			LOGGER.error("Distribution Failed for Batch JOB: {} due to: {}", jobExecutionId, e.getLocalizedMessage());
		}
		if(distributionResponse != null) {
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(summaryDTO.getGlobalList(),jobExecutionId,activityCode,obj.getAccess_token());
		}
		LOGGER.info("=======================================================================================");
		String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, null, null);
		processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
    }

	private DistributionResponse processGlobalList(List<StudentCredentialDistribution> cList, Long batchId, Map<String,DistributionPrintRequest> mapDist,String activityCode,String accessToken) {
    	List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().toList();
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && scd.getPaperType().compareTo("YED4") == 0).toList();
			List<StudentCredentialDistribution> yed2List = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && scd.getPaperType().compareTo("YED2") == 0).toList();
			List<StudentCredentialDistribution> yedrList = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && scd.getPaperType().compareTo("YEDR") == 0).toList();
			List<StudentCredentialDistribution> yedbList = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && scd.getPaperType().compareTo("YEDB") == 0).toList();
			List<StudentCredentialDistribution> studentList = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen())).toList();
			supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,null);
			schoolDistributionPrintFile(studentList,batchId,usl,mapDist);
			supportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",null);
			supportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",null);
			supportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",null);
		});
		// TODO: handle failure here
		return restUtils.mergeAndUpload(batchId,accessToken,mapDist,activityCode,null);
	}

	private void schoolDistributionPrintFile(List<StudentCredentialDistribution> studentList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		if(!studentList.isEmpty()) {
			SchoolDistributionRequest tpReq = new SchoolDistributionRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(studentList.size());
			tpReq.setStudentList(studentList);
			if(mapDist.get(usl) != null) {
				DistributionPrintRequest dist = mapDist.get(usl);
				dist.setSchoolDistributionRequest(tpReq);
				mapDist.put(usl,dist);
			}else{
				DistributionPrintRequest dist = new DistributionPrintRequest();
				dist.setSchoolDistributionRequest(tpReq);
				mapDist.put(usl,dist);
			}
		}
	}

	private void updateBackStudentRecords(List<StudentCredentialDistribution> cList,Long batchId,String activityCode,String accessToken) {
 		//TODO: catch and report errors here
        cList.forEach(scd-> {
            restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,accessToken);
            restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,accessToken);
        });
    }
}
