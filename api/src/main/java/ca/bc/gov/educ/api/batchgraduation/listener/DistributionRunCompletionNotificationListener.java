package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
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
		DistributionResponse distributionResponse = null;
		try {
			// TODO: processGlobalList should do a fire and forget to distribution api and finish.
			// then when distribution api is completed (failed or not) should callback to a new
			// endpoint on batch-api so that the rest of the process can proceed
			distributionResponse = processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),activityCode,tokenResponse.getAccess_token());
			// TODO: Create callback for this part below
			if(distributionResponse != null && distributionResponse.getMergeProcessResponse().toLowerCase().contains("successful")){
				ResponseObj obj = restUtils.getTokenResponseObject();
				Map<String, ServiceException> unprocessed = updateBackStudentRecords(summaryDTO.getGlobalList(),jobExecutionId,activityCode);
				if(!unprocessed.isEmpty()){
					status = BatchStatusEnum.FAILED.name();
					this.handleUnprocessedErrors(unprocessed);
				}
			} else {
				status = BatchStatusEnum.FAILED.name();
				LOGGER.error("Distribution Failed for Batch JOB: {} due to: {}", jobExecutionId, Optional.of(distributionResponse.getMergeProcessResponse()).orElse("response was null"));
			}
		} catch (Exception e) {
			status = BatchStatusEnum.FAILED.name();
			LOGGER.error("Distribution Failed for Batch JOB: {} due to: {}", jobExecutionId, e.getLocalizedMessage());
		}
		LOGGER.info("=======================================================================================");
		String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, null, null);
		processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
    }

	private void handleUnprocessedErrors(Map<String, ServiceException> unprocessed) {
		unprocessed.forEach((k, v) -> LOGGER.error("Student with id: {} did not have distribution date updated during monthly run due to: {}", k, v.getLocalizedMessage()));
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

	private Map<String, ServiceException> updateBackStudentRecords(List<StudentCredentialDistribution> cList,Long batchId,String activityCode) {
		Map<String, ServiceException> unprocessedStudents = new HashMap<>();
        cList.forEach(scd-> {
			try {
				final String token = restUtils.getTokenResponseObject().getAccess_token();
				restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,token);
				restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,token);
			} catch (Exception e) {
				unprocessedStudents.put(scd.getStudentID().toString(), (ServiceException) e);
			}
		});
		return unprocessedStudents;
    }
}
