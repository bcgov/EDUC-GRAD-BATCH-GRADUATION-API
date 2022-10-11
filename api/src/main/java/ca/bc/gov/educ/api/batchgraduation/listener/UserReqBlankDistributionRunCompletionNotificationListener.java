package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.BlankDistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.DistributionPrintRequest;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
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
public class UserReqBlankDistributionRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqBlankDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	@Autowired
	private GradBatchHistoryService gradBatchHistoryService;

	@Autowired
	private TaskSchedulingService taskSchedulingService;

	@Autowired RestUtils restUtils;
    
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
			String credentialType = jobParameters.getString("credentialType");
			String localDownLoad = jobParameters.getString("LocalDownload");
			String properName = jobParameters.getString("properName");
			String userScheduledId = jobParameters.getString("userScheduled");
			if(userScheduledId != null) {
				taskSchedulingService.updateUserScheduledJobs(userScheduledId);
			}
			BlankDistributionSummaryDTO summaryDTO = (BlankDistributionSummaryDTO) jobContext.get("blankDistributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new BlankDistributionSummaryDTO();
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
			ent.setLocalDownload(localDownLoad);

			gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
			
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(LOG_SEPARATION_SINGLE);
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
			LOGGER.info(LOG_SEPARATION_SINGLE);
			BlankDistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
			processGlobalList(credentialType,summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),obj.getAccess_token(),localDownLoad,properName);
			LOGGER.info(LOG_SEPARATION);
		}
    }

	private void processGlobalList(String credentialType, List<BlankCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String accessToken,String localDownload,String properName) {
		List<String> uniqueSchoolList = cList.stream().map(BlankCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		uniqueSchoolList.forEach(usl->{
			List<BlankCredentialDistribution> yed4List = new ArrayList<>();
			List<BlankCredentialDistribution> yed2List = new ArrayList<>();
			List<BlankCredentialDistribution> yedrList = new ArrayList<>();
			List<BlankCredentialDistribution> yedbList = new ArrayList<>();

			if(credentialType != null) {
				if (credentialType.equalsIgnoreCase("OT")) {
					yed4List = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YED4") == 0).collect(Collectors.toList());
				}

				if (credentialType.equalsIgnoreCase("OC")) {
					yed2List = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YED2") == 0).collect(Collectors.toList());
					yedrList = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YEDR") == 0).collect(Collectors.toList());
					yedbList = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && scd.getPaperType().compareTo("YEDB") == 0).collect(Collectors.toList());
				}
			}

			SupportListener.blankTranscriptPrintFile(yed4List,batchId,usl,mapDist,properName);
			SupportListener.blankCertificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",properName);
			SupportListener.blankCertificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",properName);
			SupportListener.blankCertificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",properName);
		});
		restUtils.createBlankCredentialsAndUpload(batchId, accessToken, mapDist,localDownload);
	}

}
