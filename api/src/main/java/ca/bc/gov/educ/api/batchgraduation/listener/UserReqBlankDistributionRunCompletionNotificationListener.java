package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
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

	@Autowired BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;
	@Autowired BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;
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
			BlankDistributionSummaryDTO summaryDTO = (BlankDistributionSummaryDTO) jobContext.get("blankDistributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new BlankDistributionSummaryDTO();
			}
			int failedRecords = summaryDTO.getErrors().size();			
			Long processedStudents = summaryDTO.getProcessedCount();
			Long expectedStudents = summaryDTO.getReadCount();
			ResponseObj obj = restUtils.getTokenResponseObject();
			processGlobalList(credentialType,summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),obj.getAccess_token());
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

			batchGradAlgorithmJobHistoryRepository.save(ent);
			
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(LOG_SEPARATION_SINGLE);
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
			List<BatchGradAlgorithmErrorHistoryEntity> eList = new ArrayList<>();
			summaryDTO.getErrors().forEach(e -> {
				LOGGER.info(" Student ID : {}, Reason: {}, Detail: {}", e.getStudentID(), e.getReason(), e.getDetail());
				BatchGradAlgorithmErrorHistoryEntity errorHistory = new BatchGradAlgorithmErrorHistoryEntity();
				errorHistory.setStudentID(UUID.fromString(e.getStudentID()));
				errorHistory.setJobExecutionId(jobExecutionId);
				errorHistory.setError(e.getReason() + "-" + e.getDetail());
				eList.add(errorHistory);
			});
			if(!eList.isEmpty())
				batchGradAlgorithmErrorHistoryRepository.saveAll(eList);

			LOGGER.info(LOG_SEPARATION_SINGLE);
			BlankDistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));
			LOGGER.info(LOG_SEPARATION);
		}
    }

	private void processGlobalList(String credentialType, List<BlankCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String accessToken) {
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

			transcriptPrintFile(yed4List,batchId,usl,mapDist);
			certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2");
			certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR");
			certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB");
		});
		restUtils.createBlankCredentialsAndUpload(batchId, accessToken, mapDist);
	}

	private void transcriptPrintFile(List<BlankCredentialDistribution> yed4List, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		if(!yed4List.isEmpty()) {
			TranscriptPrintRequest tpReq = new TranscriptPrintRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(yed4List.size());
			tpReq.setBlankTranscriptList(yed4List);
			if(mapDist.get(usl) != null) {
				DistributionPrintRequest dist = mapDist.get(usl);
				dist.setTranscriptPrintRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}else{
				DistributionPrintRequest dist = new DistributionPrintRequest();
				dist.setTranscriptPrintRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}
		}
	}

	private void certificatePrintFile(List<BlankCredentialDistribution> cList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist, String certificatePaperType) {
		if(!cList.isEmpty()) {
			CertificatePrintRequest tpReq = new CertificatePrintRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(cList.size());
			tpReq.setBlankCertificateList(cList);
			if(mapDist.get(usl) != null) {
				DistributionPrintRequest dist = mapDist.get(usl);
				if(certificatePaperType.compareTo("YED2") == 0)
					dist.setYed2CertificatePrintRequest(tpReq);
				if(certificatePaperType.compareTo("YEDR") == 0)
					dist.setYedrCertificatePrintRequest(tpReq);
				if(certificatePaperType.compareTo("YEDB") == 0)
					dist.setYedbCertificatePrintRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}else{
				DistributionPrintRequest dist = new DistributionPrintRequest();
				if(certificatePaperType.compareTo("YED2") == 0)
					dist.setYed2CertificatePrintRequest(tpReq);
				if(certificatePaperType.compareTo("YEDR") == 0)
					dist.setYedrCertificatePrintRequest(tpReq);
				if(certificatePaperType.compareTo("YEDB") == 0)
					dist.setYedbCertificatePrintRequest(tpReq);
				dist.setTotal(dist.getTotal()+1);
				mapDist.put(usl,dist);
			}
		}
	}
}
