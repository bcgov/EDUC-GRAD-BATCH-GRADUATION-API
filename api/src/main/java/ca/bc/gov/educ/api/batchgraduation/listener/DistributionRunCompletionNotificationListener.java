package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunCompletionNotificationListener.class);

	@Autowired
	SupportListener supportListener;

    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Distribution Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String jobType = jobParameters.getString("jobType");
			String activityCode = jobType != null && jobType.equalsIgnoreCase("DISTRUN")?"MONTHLYDIST":"YEARENDDIST";
			String studentSearchRequest = jobParameters.getString("searchRequest");

			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new DistributionSummaryDTO();
				summaryDTO.initializeCredentialCountMap();
			}

			// display Summary Details
			LOGGER.info("Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info("Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, null, null);

			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(" --------------------------------------------------------------------------------------");
			DistributionSummaryDTO finalSummaryDTO = summaryDTO;
			summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

			ResponseObj obj = restUtils.getTokenResponseObject();
			LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
			processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),activityCode,obj.getAccess_token());
			LOGGER.info("=======================================================================================");
		}
    }

	private void processGlobalList(List<StudentCredentialDistribution> cList, Long batchId, Map<String,DistributionPrintRequest> mapDist,String activityCode,String accessToken) {
		List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YED4") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> yed2List = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YED2") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> yedrList = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YEDR") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> yedbList = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && scd.getPaperType().compareTo("YEDB") == 0).collect(Collectors.toList());
			List<StudentCredentialDistribution> studentList = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0).collect(Collectors.toList());
			supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,null);
			schoolDistributionPrintFile(studentList,batchId,usl,mapDist);
			supportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",null);
			supportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",null);
			supportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",null);
		});
		DistributionResponse disres = restUtils.mergeAndUpload(batchId,accessToken,mapDist,activityCode,null);
		if(disres != null) {
			ResponseObj obj = restUtils.getTokenResponseObject();
			updateBackStudentRecords(cList,batchId,activityCode,obj.getAccess_token());
		}
	}

	private void updateBackStudentRecords(List<StudentCredentialDistribution> cList,Long batchId,String activityCode,String accessToken) {
        cList.forEach(scd-> {
            restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,accessToken);
            restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,accessToken);
        });
    }
}
