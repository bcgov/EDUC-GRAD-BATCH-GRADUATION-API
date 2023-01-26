package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserReqDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	@Autowired
	private TaskSchedulingService taskSchedulingService;

	ParallelDataFetch parallelDataFetch;

    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info(LOG_SEPARATION);
	    	LOGGER.info("Distribution Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			String localDownLoad = jobParameters.getString("LocalDownload");
			String credentialType = jobParameters.getString("credentialType");
			String properName = jobParameters.getString("properName");
			String studentSearchRequest = jobParameters.getString("searchRequest");
			String userScheduledId = jobParameters.getString("userScheduled");
			if(userScheduledId != null) {
				taskSchedulingService.updateUserScheduledJobs(userScheduledId);
			}

			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new DistributionSummaryDTO();
				summaryDTO.initializeCredentialCountMap();
			}

			// display Summary Details
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(LOG_SEPARATION_SINGLE);
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, TaskSelection.URDBJ, credentialType);

			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(LOG_SEPARATION_SINGLE);
			DistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
			processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),credentialType,obj.getAccess_token(),localDownLoad,properName);
			LOGGER.info(LOG_SEPARATION);
		}
    }

	private void processGlobalList(List<StudentCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String credentialType, String accessToken,String localDownload,String properName) {
		List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = new ArrayList<>();
			List<StudentCredentialDistribution> yed2List = new ArrayList<>();
			List<StudentCredentialDistribution> yedrList = new ArrayList<>();
			List<StudentCredentialDistribution> yedbList = new ArrayList<>();
			if(credentialType != null) {
				if (credentialType.equalsIgnoreCase("OT") || credentialType.equalsIgnoreCase("RT")) {
					yed4List = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YED4") == 0).collect(Collectors.toList());
				}

				if (credentialType.equalsIgnoreCase("OC") || credentialType.equalsIgnoreCase("RC")) {
					yed2List = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YED2") == 0).collect(Collectors.toList());
					yedrList = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YEDR") == 0).collect(Collectors.toList());
					yedbList = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YEDB") == 0).collect(Collectors.toList());
				}
			}
			SupportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,properName);
			SupportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",properName);
			SupportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",properName);
			SupportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",properName);
		});
		DistributionResponse disres = null;
		String activityCode = null;
		if(credentialType != null) {
			if(credentialType.equalsIgnoreCase("OC")) {
				activityCode = "USERDISTOC";
			} else {
				activityCode = credentialType.equalsIgnoreCase("OT")?"USERDISTOT":"USERDISTRC";
			}
			if (credentialType.equalsIgnoreCase("RC")) {
				disres = restUtils.createReprintAndUpload(batchId, accessToken, mapDist,activityCode,localDownload);
			} else {
				disres = restUtils.mergeAndUpload(batchId, accessToken, mapDist,activityCode,localDownload);
			}
		}
		if(disres != null) {
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(cList,batchId,activityCode,obj.getAccess_token());
		}
	}

	private void updateBackStudentRecords(List<StudentCredentialDistribution> cList, Long batchId,String activityCode, String accessToken) {
		cList.forEach(scd-> {
			restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,accessToken);
			restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,accessToken);
		});
	}

}
