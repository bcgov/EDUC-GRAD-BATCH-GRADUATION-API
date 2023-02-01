package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
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

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserReqDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	@Autowired
	TaskSchedulingService taskSchedulingService;
	@Autowired
	GraduationReportService graduationReportService;
	@Autowired
	RestUtils restUtils;
	@Autowired
	SupportListener supportListener;

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

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, TaskSelection.URDBJ, credentialType);

			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);

			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process " + LOG_SEPARATION_SINGLE);
			processGlobalList(summaryDTO,jobExecutionId,credentialType,obj.getAccess_token(),localDownLoad,properName);

			DistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			// display Summary Details
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(LOG_SEPARATION_SINGLE);
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
			LOGGER.info(LOG_SEPARATION_SINGLE);

			LOGGER.info(LOG_SEPARATION);
		}
    }

	private void processGlobalList(DistributionSummaryDTO summaryDTO, Long batchId, String credentialType, String accessToken,String localDownload,String properName) {
		List<StudentCredentialDistribution> cList = summaryDTO.getGlobalList();
		Map<String, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
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
			supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,properName);
			supportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",properName);
			supportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",properName);
			supportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",properName);
		});
		DistributionResponse disres = null;
		String activityCode = null;
		if(credentialType != null) {
			if(credentialType.equalsIgnoreCase("OC")) {
				activityCode = "USERDISTOC";
				/** GRADT-553
				 *  User Request Distribution Run - Original Certificate OC
				 ****  Also select the students’ transcript for print
				 */
				addTranscriptsToDistributionRequest(cList,summaryDTO,batchId,properName);
			} else {
				activityCode = credentialType.equalsIgnoreCase("OT")?"USERDISTOT":"USERDISTRC";
			}
			if (credentialType.equalsIgnoreCase("RC")) {
				disres = restUtils.createReprintAndUpload(batchId, accessToken, mapDist, activityCode,localDownload);
			} else {
				disres = restUtils.mergeAndUpload(batchId, accessToken, mapDist, activityCode,localDownload);
			}
		}
		if(disres != null) {
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(cList,batchId,activityCode,obj.getAccess_token());
		}
	}

	private void addTranscriptsToDistributionRequest(List<StudentCredentialDistribution> cList, DistributionSummaryDTO summaryDTO, Long batchId, String properName) {
		Map<String, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
		mapDist.forEach((schoolCode, distributionPrintRequest) -> {
			List<StudentCredentialDistribution> mergedCertificateList = distributionPrintRequest.getMergedListOfCertificates();
			List<StudentCredentialDistribution> uniqueCertificateList = mergedCertificateList.stream().distinct().collect(Collectors.toList());
			List<String> studentPens = uniqueCertificateList.stream().map(StudentCredentialDistribution::getPen).collect(Collectors.toList());
			StudentSearchRequest searchRequest = StudentSearchRequest.builder().pens(studentPens).build();
			List<StudentCredentialDistribution> transcriptDistributionList = restUtils.getStudentsForUserReqDisRun("OT",searchRequest,restUtils.getTokenResponseObject().getAccess_token());
			for(StudentCredentialDistribution certScd: uniqueCertificateList) {
				for(StudentCredentialDistribution trScd: transcriptDistributionList) {
					if(certScd.getStudentID().equals(trScd.getStudentID())) {
						trScd.setSchoolOfRecord(certScd.getSchoolOfRecord());
						trScd.setPen(certScd.getPen());
						trScd.setLegalFirstName(certScd.getLegalFirstName());
						trScd.setLegalMiddleNames(certScd.getLegalMiddleNames());
						trScd.setLegalLastName(certScd.getLegalLastName());
						trScd.setProgramCompletionDate(certScd.getProgramCompletionDate());
						trScd.setHonoursStanding(certScd.getHonoursStanding());
						trScd.setProgram(certScd.getProgram());
						trScd.setStudentGrade(certScd.getStudentGrade());
						trScd.setNonGradReasons(certScd.getNonGradReasons());
						summaryDTO.increment(trScd.getPaperType());
					}
				}
			}
			cList.addAll(transcriptDistributionList);
			supportListener.transcriptPrintFile(transcriptDistributionList, batchId, schoolCode, mapDist, properName);
		});
	}

	private void updateBackStudentRecords(List<StudentCredentialDistribution> cList, Long batchId,String activityCode, String accessToken) {
		cList.forEach(scd-> {
			restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,accessToken);
		});
		List<UUID> studentIDs = cList.stream().map(StudentCredentialDistribution::getStudentID).distinct().collect(Collectors.toList());
		studentIDs.forEach(sid-> {
			restUtils.updateStudentGradRecord(sid,batchId,activityCode,accessToken);
		});
	}

}
