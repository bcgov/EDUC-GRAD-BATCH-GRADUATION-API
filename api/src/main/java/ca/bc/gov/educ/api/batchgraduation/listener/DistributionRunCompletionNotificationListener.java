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

import java.util.*;

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
		try {
			// GRAD2-2017: fire and forget to distribution api and finish.
			processGlobalList(jobExecutionId,summaryDTO.getMapDist(),activityCode,tokenResponse.getAccess_token());
		} catch (Exception e) {
			LOGGER.error("Distribution Failed for Batch JOB: {} due to: {}", jobExecutionId, e.getLocalizedMessage());
		}
		LOGGER.info("=======================================================================================");
    }

	private void processGlobalList(Long batchId, Map<String,DistributionPrintRequest> mapDist,String activityCode,String accessToken) {
    	List<StudentCredentialDistribution> cList = distributionService.getStudentCredentialDistributions(batchId);
		LOGGER.info("list size =  {}", cList.size());
		List<String> uniqueSchoolList = distributionService.getSchoolListForDistribution(batchId);
		LOGGER.info("unique schools =  {}", uniqueSchoolList.size());
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
		if (!cList.isEmpty())
			restUtils.mergeAndUpload(batchId,accessToken,mapDist,activityCode,null);
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
}
