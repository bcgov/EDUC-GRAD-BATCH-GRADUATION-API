package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
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

import static ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum.*;
import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;
import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortSchoolBySchoolOfRecordId;
import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortStudentCredentialDistributionBySchoolAndNames;

@Component
public class UserReqDistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqDistributionRunCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";
    private static final String LOG_SEPARATION_SINGLE = " --------------------------------------------------------------------------------------";

	@Autowired
	GraduationReportService graduationReportService;
	@Autowired
	RestUtils restUtils;
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
			String localDownLoad = jobParameters.getString("LocalDownload");
			String credentialType = jobParameters.getString("credentialType");
			String properName = jobParameters.getString("properName");
			String studentSearchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
			updateUserSchedulingJobs(jobParameters);

			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new DistributionSummaryDTO();
				summaryDTO.initializeCredentialCountMap();
			}

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, TaskSelection.URDBJ, credentialType);

			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process " + LOG_SEPARATION_SINGLE);

			StudentSearchRequest studentSearchRequestObject = (StudentSearchRequest)jsonTransformer.unmarshall(studentSearchRequest, StudentSearchRequest.class);
			summaryDTO.setStudentSearchRequest(studentSearchRequestObject);
			DistributionResponse distResponse = processGlobalList(summaryDTO,jobExecutionId,credentialType,obj.getAccess_token(),localDownLoad,StringUtils.defaultIfBlank(properName, studentSearchRequestObject.getUser()));
			String status;
			if(distResponse != null) {
				status  = FAILED.name().equalsIgnoreCase(distResponse.getMergeProcessResponse()) ? FAILED.name(): credentialType.equalsIgnoreCase("RC") ? COMPLETED.name() : STARTED.name();
			} else {
				status = COMPLETED.name();
			}
			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);

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

	private DistributionResponse processGlobalList(DistributionSummaryDTO summaryDTO, Long batchId, String credentialType, String accessToken,String localDownload,String properName) {
		List<StudentCredentialDistribution> cList = summaryDTO.getGlobalList();
		sortStudentCredentialDistributionBySchoolAndNames(cList);
		Map<UUID, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
    	List<UUID> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolId).distinct().collect(Collectors.toList());
		sortSchoolBySchoolOfRecordId(uniqueSchoolList);
		List<StudentCredentialDistribution> studentCredentialDistributionControlList = new ArrayList<>();
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = new ArrayList<>();
			List<StudentCredentialDistribution> yed2List = new ArrayList<>();
			List<StudentCredentialDistribution> yedrList = new ArrayList<>();
			List<StudentCredentialDistribution> yedbList = new ArrayList<>();
			if(credentialType != null) {
				if (credentialType.equalsIgnoreCase("OT") || credentialType.equalsIgnoreCase("RT")) {
					yed4List = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YED4".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
				}
				if (credentialType.equalsIgnoreCase("OC") || credentialType.equalsIgnoreCase("RC")) {
					yed2List = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YED2".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
					yedrList = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YEDR".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
					yedbList = cList.stream().filter(scd -> scd.getSchoolId().compareTo(usl) == 0 && "YEDB".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
				}
			}

			supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,properName);
			supportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",properName);
			supportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",properName);
			supportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",properName);

			studentCredentialDistributionControlList.addAll(yed4List);
			studentCredentialDistributionControlList.addAll(yed2List);
			studentCredentialDistributionControlList.addAll(yedrList);
			studentCredentialDistributionControlList.addAll(yedbList);

		});
		String activityCode;
		if(credentialType != null) {
			if("OC".equalsIgnoreCase(credentialType)) {
				activityCode = "USERDISTOC";
				/** GRADT-553
				 *  User Request Distribution Run - Original Certificate OC
				 ****  Also select the students’ transcript for print
				 */
				addTranscriptsToDistributionRequest(studentCredentialDistributionControlList, cList,summaryDTO,batchId,properName);
			} else {
				activityCode = "OT".equalsIgnoreCase(credentialType) ? "USERDISTOT" : "USERDISTRC";
			}
			if(!studentCredentialDistributionControlList.isEmpty()) {
				DistributionResponse disres = null;
				DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).activityCode(activityCode).studentSearchRequest(summaryDTO.getStudentSearchRequest()).build();
				if (credentialType.equalsIgnoreCase("RC")) {
					disres = restUtils.createReprintAndUpload(batchId, accessToken, distributionRequest, activityCode, localDownload);
				} else {
					disres = restUtils.mergeAndUpload(batchId, distributionRequest, activityCode, localDownload);
				}
				if(disres != null) {
					 if(!FAILED.name().equals(disres.getMergeProcessResponse())) {
						 updateBackStudentRecords(studentCredentialDistributionControlList.stream().distinct().toList(),batchId,activityCode);
					 }
					 LOGGER.info("Merge and Upload Status {}", disres.getMergeProcessResponse());
				}
				return disres;
			}
		}
		return null;
	}

	private void addTranscriptsToDistributionRequest(List<StudentCredentialDistribution> controlList, List<StudentCredentialDistribution> cList, DistributionSummaryDTO summaryDTO, Long batchId, String properName) {
		Map<UUID, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
		mapDist.forEach((schoolCode, distributionPrintRequest) -> {
			List<StudentCredentialDistribution> mergedCertificateList = distributionPrintRequest.getMergedListOfCertificates();
			List<StudentCredentialDistribution> uniqueCertificateList = mergedCertificateList.stream().distinct().collect(Collectors.toList());
			List<String> studentPens = uniqueCertificateList.stream().map(StudentCredentialDistribution::getPen).collect(Collectors.toList());
			List<UUID> studentIDs = uniqueCertificateList.stream().map(StudentCredentialDistribution::getStudentID).collect(Collectors.toList());
			StudentSearchRequest searchRequest = StudentSearchRequest.builder().pens(studentPens).studentIDs(studentIDs).build();
			if(LOGGER.isDebugEnabled()) {
				String studentSearchRequest = jsonTransformer.marshall(searchRequest);
				LOGGER.debug("Get {} students credentials for the pens: {}", "OT", studentSearchRequest);
			}
			List<StudentCredentialDistribution> transcriptDistributionList = restUtils.getStudentsForUserReqDisRun("OT",searchRequest);
			for(StudentCredentialDistribution certScd: uniqueCertificateList) {
				for(StudentCredentialDistribution trScd: transcriptDistributionList) {
					if(certScd.getStudentID().equals(trScd.getStudentID())) {
						trScd.setSchoolOfRecord(certScd.getSchoolOfRecord());
						trScd.setSchoolId(certScd.getSchoolId());
						trScd.setPen(certScd.getPen());
						trScd.setLegalFirstName(certScd.getLegalFirstName());
						trScd.setLegalMiddleNames(certScd.getLegalMiddleNames());
						trScd.setLegalLastName(certScd.getLegalLastName());
						trScd.setProgramCompletionDate(certScd.getProgramCompletionDate());
						trScd.setHonoursStanding(certScd.getHonoursStanding());
						trScd.setProgram(certScd.getProgram());
						trScd.setStudentGrade(certScd.getStudentGrade());
						trScd.setNonGradReasons(certScd.getNonGradReasons());
						trScd.setLastUpdateDate(certScd.getLastUpdateDate());
						summaryDTO.increment(trScd.getPaperType());
					}
				}
			}
			cList.addAll(transcriptDistributionList);
			controlList.addAll(transcriptDistributionList);
			supportListener.transcriptPrintFile(transcriptDistributionList, batchId, schoolCode, mapDist, properName);
		});
	}

	private void updateBackStudentRecords(Collection<StudentCredentialDistribution> cList, Long batchId,String activityCode) {
		cList.forEach(scd-> {
			LOGGER.debug("Update back Student Record {}", scd.getStudentID());
			String accessToken = restUtils.fetchAccessToken();
			restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,accessToken);
		});
		List<UUID> studentIDs = cList.stream().map(StudentCredentialDistribution::getStudentID).distinct().toList();
		studentIDs.forEach(sid-> {
			restUtils.updateStudentGradRecord(sid,batchId,activityCode);
		});
	}

}
