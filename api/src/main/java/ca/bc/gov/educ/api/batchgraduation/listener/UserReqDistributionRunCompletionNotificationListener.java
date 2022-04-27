package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserReqDistributionRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserReqDistributionRunCompletionNotificationListener.class);
    
    @Autowired
    private BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

	@Autowired
	private BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;
    
    @Autowired
    private RestUtils restUtils;

	private ParallelDataFetch parallelDataFetch;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Distribution Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());
	    	JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");

			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new DistributionSummaryDTO();
			}
			int failedRecords = summaryDTO.getErrors().size();			
			Long processedStudents = summaryDTO.getProcessedCount();
			Long expectedStudents = summaryDTO.getReadCount();
			ResponseObj obj = restUtils.getTokenResponseObject();
			processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),summaryDTO.getCredentialType(),obj.getAccess_token());
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
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Errors:		   {}", summaryDTO.getErrors().size());
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

			LOGGER.info(" --------------------------------------------------------------------------------------");
			DistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key));
			});
			LOGGER.info("=======================================================================================");
		}else if (jobExecution.getStatus() == BatchStatus.FAILED) {
			long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Distribution Job failed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());

			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			
			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new DistributionSummaryDTO();
			}
			int failedRecords = 0;			
			Mono<DistributionDataParallelDTO> list = parallelDataFetch.fetchDistributionRequiredData(summaryDTO.getAccessToken());
			DistributionDataParallelDTO parallelDTO = list.block();
			if(parallelDTO != null ) {
				failedRecords = parallelDTO.certificateList().size()+parallelDTO.transcriptList().size();
			}	
			
			Long processedStudents = summaryDTO.getProcessedCount();
			Long expectedStudents = summaryDTO.getReadCount();			
			
			BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
			ent.setActualStudentsProcessed(processedStudents);
			ent.setExpectedStudentsProcessed(expectedStudents);
			ent.setFailedStudentsProcessed(failedRecords);
			ent.setJobExecutionId(jobExecutionId);
			ent.setStartTime(startTime);
			ent.setEndTime(endTime);
			ent.setStatus(status);
			
			batchGradAlgorithmJobHistoryRepository.save(ent);
			
			LOGGER.info(" Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info(" Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info(" Errors		 : {}", summaryDTO.getErrors().size());
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
			LOGGER.info(" --------------------------------------------------------------------------------------");
			DistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().entrySet().stream().forEach(e -> {
				String key = e.getKey();
				LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key));
			});
			LOGGER.info("=======================================================================================");
		}
    }

	private void processGlobalList(List<StudentCredentialDistribution> cList, Long batchId, Map<String, DistributionPrintRequest> mapDist, String credentialType, String accessToken) {
		List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		List<StudentCredentialDistribution> finalCList = cList;
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YED4") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> yed2List = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YED2") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> yedrList = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YEDR") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> yedbList = finalCList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YEDB") == 0).collect(Collectors.toList());
			transcriptPrintFile(yed4List,batchId,usl,mapDist);
			certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2");
			certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR");
			certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB");
		});
		DistributionResponse disres = null;
		if(credentialType.equalsIgnoreCase("RC")) {
			disres = restUtils.createReprintAndUpload(batchId, accessToken, mapDist);
		}else {
			disres = restUtils.mergeAndUpload(batchId, accessToken, mapDist);
		}
//		if(disres != null) {
//			updateBackStudentRecords(cList,accessToken);
//		}
	}

	private void updateBackStudentRecords(List<StudentCredentialDistribution> cList,String accessToken) {
		cList.forEach(scd-> {
			restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),accessToken);
		});
	}
	private void transcriptPrintFile(List<StudentCredentialDistribution> yed4List, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		if(!yed4List.isEmpty()) {
			TranscriptPrintRequest tpReq = new TranscriptPrintRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(yed4List.size());
			tpReq.setTranscriptList(yed4List);
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

	private void certificatePrintFile(List<StudentCredentialDistribution> cList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist, String certificatePaperType) {
		if(!cList.isEmpty()) {
			CertificatePrintRequest tpReq = new CertificatePrintRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(cList.size());
			tpReq.setCertificateList(cList);
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
