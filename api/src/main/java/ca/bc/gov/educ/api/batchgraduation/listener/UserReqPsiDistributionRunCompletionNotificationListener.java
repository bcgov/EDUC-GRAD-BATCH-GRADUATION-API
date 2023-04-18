package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
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
import java.util.stream.Collectors;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;

@Component
public class UserReqPsiDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqPsiDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	private TaskSchedulingService taskSchedulingService;
	private SupportListener supportListener;

	@Autowired
	public UserReqPsiDistributionRunCompletionNotificationListener(TaskSchedulingService taskSchedulingService, SupportListener supportListener) {
		this.taskSchedulingService = taskSchedulingService;
		this.supportListener = supportListener;
	}
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
		LOGGER.info(LOG_SEPARATION);
		LOGGER.info("PSI Distribution Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
		JobParameters jobParameters = jobExecution.getJobParameters();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		Long jobExecutionId = jobExecution.getId();
		String status = jobExecution.getStatus().toString();
		Date startTime = jobExecution.getStartTime();
		Date endTime = jobExecution.getEndTime();
		String jobTrigger = jobParameters.getString(EducGradBatchGraduationApiConstants.JOB_TRIGGER);
		String jobType = jobParameters.getString(EducGradBatchGraduationApiConstants.JOB_TYPE);
		String transmissionType = jobParameters.getString(EducGradBatchGraduationApiConstants.TRANSMISSION_TYPE);
		String studentSearchRequest = jobParameters.getString(EducGradBatchGraduationApiConstants.SEARCH_REQUEST);

		String userScheduledId = jobParameters.getString(EducGradBatchGraduationApiConstants.USER_SCHEDULED);
		if(userScheduledId != null) {
			taskSchedulingService.updateUserScheduledJobs(userScheduledId);
		}

		PsiDistributionSummaryDTO summaryDTO = (PsiDistributionSummaryDTO)jobContext.get("psiDistributionSummaryDTO");
		if(summaryDTO == null) {
			summaryDTO = new PsiDistributionSummaryDTO();
			summaryDTO.initializeCredentialCountMap();
		}

		// display Summary Details
		LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
		LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
		LOGGER.info(LOG_SEPARATION_SINGLE);
		LOGGER.info("Errors:{}", summaryDTO.getErrors().size());

		String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, TaskSelection.URPDBJ, transmissionType);


		LOGGER.info(LOG_SEPARATION_SINGLE);
		PsiDistributionSummaryDTO finalSummaryDTO = summaryDTO;
		summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

		ResponseObj obj = restUtils.getTokenResponseObject();
		LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
		try {
			processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),obj.getAccess_token(),transmissionType);
		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage());
			status = "FAILED";
		}
		// save batch job & error history
		processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
		LOGGER.info(LOG_SEPARATION);
    }

	private void processGlobalList(List<PsiCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String accessToken,String transmissionType) {
		List<String> uniquePSIList = cList.stream().map(PsiCredentialDistribution::getPsiCode).distinct().toList();
		uniquePSIList.forEach(upl->{
			List<PsiCredentialDistribution> yed4List = cList.stream().filter(scd -> scd.getPsiCode().compareTo(upl) == 0).collect(Collectors.toList());
			supportListener.psiPrintFile(yed4List,batchId,upl,mapDist);
		});
		String localDownload =  StringUtils.equalsIgnoreCase(transmissionType, "FTP")?"Y":"N";
		//Grad2-1931 added transmissionType
		DistributionResponse disres = restUtils.mergePsiAndUpload(batchId, accessToken, mapDist,localDownload,transmissionType);
		if(disres != null) {
			String activityCode = StringUtils.equalsIgnoreCase(transmissionType, "PAPER")?"USERDISTPSIP":"USERDISTPISF";
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(cList,batchId,activityCode,obj.getAccess_token());
		}
	}

	private void updateBackStudentRecords(List<PsiCredentialDistribution> cList, Long batchId,String activityCode, String accessToken) {
		cList.forEach(scd->	restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,accessToken));
	}
}
