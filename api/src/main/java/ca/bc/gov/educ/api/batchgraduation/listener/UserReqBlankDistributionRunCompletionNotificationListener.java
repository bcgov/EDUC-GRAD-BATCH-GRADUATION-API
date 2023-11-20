package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
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

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;
import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortBlankCredentialDistributionBySchoolAndNames;
import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortSchoolBySchoolOfRecord;

@Component
public class UserReqBlankDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqBlankDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	@Autowired
	private TaskSchedulingService taskSchedulingService;

	@Autowired
	SupportListener supportListener;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
			LOGGER.info(LOG_SEPARATION);
			JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String jobType = jobParameters.getString("jobType");
			LOGGER.info("{} Distribution Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis/1000, jobExecution.getStatus());

			String status = jobExecution.getStatus().toString();
			Date startTime = DateUtils.toDate(jobExecution.getStartTime());
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());
			String jobTrigger = jobParameters.getString("jobTrigger");
			String credentialType = jobParameters.getString("credentialType");
			String localDownLoad = jobParameters.getString("LocalDownload");
			String studentSearchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
			String userScheduledId = jobParameters.getString("userScheduled");
			if(userScheduledId != null) {
				taskSchedulingService.updateUserScheduledJobs(userScheduledId);
			}
			BlankDistributionSummaryDTO summaryDTO = (BlankDistributionSummaryDTO) jobContext.get("blankDistributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new BlankDistributionSummaryDTO();
				summaryDTO.initializeCredentialCountMap();
			}

			// display Summary Details
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(LOG_SEPARATION_SINGLE);
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, TaskSelection.BDBJ, credentialType);

			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(LOG_SEPARATION_SINGLE);
			BlankDistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			StudentSearchRequest studentSearchRequestObject = (StudentSearchRequest)jsonTransformer.unmarshall(studentSearchRequest, StudentSearchRequest.class);
			summaryDTO.setStudentSearchRequest(studentSearchRequestObject);

			String properName = StringUtils.defaultIfBlank(jobParameters.getString("properName"), studentSearchRequestObject.getUser());
			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
			processGlobalList(summaryDTO, credentialType, jobExecutionId, obj.getAccess_token(), localDownLoad, properName);
			LOGGER.info(LOG_SEPARATION);
		}
    }

	private void processGlobalList(BlankDistributionSummaryDTO summaryDTO , String credentialType, Long batchId, String accessToken,String localDownload,String properName) {
		StudentSearchRequest studentSearchRequest = summaryDTO.getStudentSearchRequest();
		List<BlankCredentialDistribution> cList = summaryDTO.getGlobalList();
		sortBlankCredentialDistributionBySchoolAndNames(cList);
		Map<String, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
		List<String> uniqueSchoolList = cList.stream().map(BlankCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		sortSchoolBySchoolOfRecord(uniqueSchoolList);
		uniqueSchoolList.forEach(usl->{
			List<BlankCredentialDistribution> yed4List = new ArrayList<>();
			List<BlankCredentialDistribution> yed2List = new ArrayList<>();
			List<BlankCredentialDistribution> yedrList = new ArrayList<>();
			List<BlankCredentialDistribution> yedbList = new ArrayList<>();

			if(credentialType != null) {
				if (StringUtils.equalsIgnoreCase(credentialType, "OT")) {
					yed4List = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && "YED4".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
				}

				if (StringUtils.equalsIgnoreCase(credentialType, "OC")) {
					yed2List = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && "YED2".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
					yedrList = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && "YEDR".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
					yedbList = cList.stream().filter(scd -> scd.getSchoolOfRecord().compareTo(usl) == 0 && "YEDB".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
				}
			}

			supportListener.blankTranscriptPrintFile(yed4List,batchId,usl,mapDist,properName);
			supportListener.blankCertificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",properName);
			supportListener.blankCertificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",properName);
			supportListener.blankCertificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",properName);
		});
		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).studentSearchRequest(studentSearchRequest).build();
		restUtils.createBlankCredentialsAndUpload(batchId, accessToken, distributionRequest,localDownload);
	}

}
