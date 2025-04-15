package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum.*;

@Component
public class UserReqPsiDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqPsiDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	private SupportListener supportListener;

	@Autowired
	public UserReqPsiDistributionRunCompletionNotificationListener(SupportListener supportListener) {
		this.supportListener = supportListener;
	}
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
		LOGGER.info(LOG_SEPARATION);
		JobParameters jobParameters = jobExecution.getJobParameters();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		Long jobExecutionId = jobExecution.getId();
		String jobType = jobParameters.getString(EducGradBatchGraduationApiConstants.JOB_TYPE);
		LOGGER.info("{} Distribution Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis/1000, jobExecution.getStatus());

		Date startTime = DateUtils.toDate(jobExecution.getStartTime());
		Date endTime = DateUtils.toDate(jobExecution.getEndTime());
		String jobTrigger = jobParameters.getString(EducGradBatchGraduationApiConstants.JOB_TRIGGER);
		String transmissionType = jobParameters.getString(EducGradBatchGraduationApiConstants.TRANSMISSION_TYPE);
		String studentSearchRequest = jobParameters.getString(EducGradBatchGraduationApiConstants.SEARCH_REQUEST, "{}");

		updateUserSchedulingJobs(jobParameters);

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
		String status  = processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),obj.getAccess_token(),transmissionType) ? COMPLETED.name(): FAILED.name();
		// save batch job & error history
		processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
		LOGGER.info(LOG_SEPARATION);
    }

	private Boolean processGlobalList(List<PsiCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String accessToken, String transmissionType) {
		List<String> uniquePSIList = cList.stream().map(PsiCredentialDistribution::getPsiCode).distinct().toList();
		uniquePSIList.forEach(psiCode->{
			List<PsiCredentialDistribution> yed4List = cList.stream().filter(scd -> scd.getPsiCode().compareTo(psiCode) == 0).toList();
			supportListener.psiPrintFile(yed4List,batchId,psiCode,mapDist);
		});
		if(!cList.isEmpty()) {
			String localDownload = StringUtils.equalsIgnoreCase(transmissionType, "FTP") ? "Y" : "N";
			//Grad2-1931 added transmissionType
			String activityCode = StringUtils.equalsIgnoreCase(transmissionType, "PAPER") ? "USERDISTPSIP" : "USERDISTPSIF";
			DistributionRequest<String> distributionRequest = DistributionRequest.<String>builder().mapDist(mapDist).activityCode(activityCode).build();
			DistributionResponse disres = restUtils.mergePsiAndUpload(batchId, accessToken, distributionRequest, localDownload, transmissionType);
			if (disres != null) {
				LOGGER.info("Merge and Upload Status {}",disres.getMergeProcessResponse());
				if(FAILED.name().equalsIgnoreCase(disres.getMergeProcessResponse())) {
					return false;
				}
				updateBackStudentRecords(cList, batchId, activityCode);
				return true;
			}
		}
		return true;
	}

	private void updateBackStudentRecords(List<PsiCredentialDistribution> cList, Long batchId,String activityCode) {
		cList.forEach(scd->	{
			LOGGER.debug("Update back Student Record {}", scd.getStudentID());
			restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode);
		});
	}
}
