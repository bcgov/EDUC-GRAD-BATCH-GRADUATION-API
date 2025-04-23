package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
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

import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum.COMPLETED;
import static ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum.FAILED;
import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@Component
public class UserReqBlankDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqBlankDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

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

			Date startTime = DateUtils.toDate(jobExecution.getStartTime());
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());
			String jobTrigger = jobParameters.getString("jobTrigger");
			String credentialType = jobParameters.getString("credentialType");
			String localDownLoad = jobParameters.getString("LocalDownload");
			String properName = jobParameters.getString("properName");
			String studentSearchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
			updateUserSchedulingJobs(jobParameters);
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

			LOGGER.info(LOG_SEPARATION_SINGLE);
			BlankDistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			StudentSearchRequest studentSearchRequestObject = (StudentSearchRequest) jsonTransformer.unmarshall(studentSearchRequest, StudentSearchRequest.class);

			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
			String status = processGlobalList(studentSearchRequestObject, credentialType, summaryDTO.getGlobalList(), jobExecutionId, summaryDTO.getMapDist(), obj.getAccess_token(), localDownLoad, StringUtils.defaultIfBlank(properName, studentSearchRequestObject.getUser())) ? COMPLETED.name() : FAILED.name();

			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(LOG_SEPARATION);
		}
    }

	private Boolean processGlobalList(StudentSearchRequest studentSearchRequest , String credentialType, List<BlankCredentialDistribution> cList, Long batchId, Map<UUID, DistributionPrintRequest> mapDist, String accessToken,String localDownload,String properName) {
		List<UUID> uniqueSchoolList = cList.stream().map(BlankCredentialDistribution::getSchoolId).distinct().collect(Collectors.toList());
		uniqueSchoolList.forEach(usl->{
			List<BlankCredentialDistribution> yed4List = new ArrayList<>();
			List<BlankCredentialDistribution> yed2List = new ArrayList<>();
			List<BlankCredentialDistribution> yedrList = new ArrayList<>();
			List<BlankCredentialDistribution> yedbList = new ArrayList<>();

			if(credentialType != null) {
				if (StringUtils.equalsIgnoreCase(credentialType, "OT")) {
					yed4List = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YED4".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
				}

				if (StringUtils.equalsIgnoreCase(credentialType, "OC")) {
					yed2List = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YED2".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
					yedrList = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YEDR".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
					yedbList = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YEDB".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
				}
			}

			supportListener.blankTranscriptPrintFile(yed4List,batchId,usl,mapDist,properName);
			supportListener.blankCertificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",properName);
			supportListener.blankCertificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",properName);
			supportListener.blankCertificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",properName);
		});
		DistributionRequest<UUID> distributionRequest = DistributionRequest.<UUID>builder().mapDist(mapDist).studentSearchRequest(studentSearchRequest).build();
		DistributionResponse disres = restUtils.createBlankCredentialsAndUpload(batchId, accessToken, distributionRequest,localDownload);
		return disres != null && disres.getMergeProcessResponse().equalsIgnoreCase(FAILED.name()) ? false : true;
	}

}
