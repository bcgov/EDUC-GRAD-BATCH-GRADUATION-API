package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserReqPsiDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqPsiDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	@Autowired
	private TaskSchedulingService taskSchedulingService;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info(LOG_SEPARATION);
	    	LOGGER.info("PSI Distribution Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			String transmissionType = jobParameters.getString("transmissionType");
			String studentSearchRequest = jobParameters.getString("searchRequest");

			String userScheduledId = jobParameters.getString("userScheduled");
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

			String jobParametersDTO = populateJobParametersDTO(jobType, transmissionType, studentSearchRequest);

			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(LOG_SEPARATION_SINGLE);
			PsiDistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
			processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),obj.getAccess_token(),transmissionType);
			LOGGER.info(LOG_SEPARATION);
		}
    }

	private String populateJobParametersDTO(String jobType, String transmissionType, String studentSearchRequest) {
		JobParametersForPsiDistribution jobParamsDto = new JobParametersForPsiDistribution();
		jobParamsDto.setJobName(jobType);
		jobParamsDto.setTransmissionType(transmissionType);

		try {
			PsiCredentialRequest payload = new ObjectMapper().readValue(studentSearchRequest, PsiCredentialRequest.class);
			jobParamsDto.setPayload(payload);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String jobParamsDtoStr = null;
		try {
			jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
		} catch (Exception e) {
			LOGGER.error("Job Parameters DTO parse error - {}", e.getMessage());
		}

		return jobParamsDtoStr != null? jobParamsDtoStr : studentSearchRequest;
	}

	private void processGlobalList(List<PsiCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String accessToken,String transmissionType) {
		List<String> uniquePSIList = cList.stream().map(PsiCredentialDistribution::getPsiCode).distinct().collect(Collectors.toList());
		uniquePSIList.forEach(upl->{
			List<PsiCredentialDistribution> yed4List = cList.stream().filter(scd -> scd.getPsiCode().compareTo(upl) == 0).collect(Collectors.toList());
			SupportListener.psiPrintFile(yed4List,batchId,upl,mapDist);
		});
		String localDownload =  StringUtils.equalsIgnoreCase(transmissionType, "FTP")?"Y":"N";
		DistributionResponse disres = restUtils.mergePsiAndUpload(batchId, accessToken, mapDist,localDownload);
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
