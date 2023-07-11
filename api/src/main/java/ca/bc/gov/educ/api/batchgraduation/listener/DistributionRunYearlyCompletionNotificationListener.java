package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.processing.Generated;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortSchoolBySchoolOfRecord;
import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortStudentCredentialDistributionBySchoolAndNames;

@Component
public class DistributionRunYearlyCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyCompletionNotificationListener.class);

	@Autowired
	SupportListener supportListener;

    @Override
	@Generated("default")
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
			LOGGER.info("=======================================================================================");
			JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String jobType = jobParameters.getString("jobType");
			LOGGER.info("{} Distribution Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis/1000, jobExecution.getStatus());

			String status = jobExecution.getStatus().toString();
			Date startTime = jobExecution.getStartTime();
			Date endTime = jobExecution.getEndTime();
			String jobTrigger = jobParameters.getString("jobTrigger");
			String searchRequest = jobParameters.getString("searchRequest");

			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new DistributionSummaryDTO();
				summaryDTO.initializeCredentialCountMap();
			}

			processGlobalList(summaryDTO, searchRequest, "YEARENDDIST");

			String studentSearchRequest = jobParameters.getString("searchRequest");
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

		}
    }

	protected void processGlobalList(DistributionSummaryDTO summaryDTO, String searchRequest, String activityCode) {
    	Long batchId = summaryDTO.getBatchId();
    	List<StudentCredentialDistribution> cList = summaryDTO.getGlobalList();
		filterStudentCredentialDistribution(cList, searchRequest);
		sortStudentCredentialDistributionBySchoolAndNames(cList);
		LOGGER.info("list size =  {}", cList.size());
    	Map<String, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
		List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
		sortSchoolBySchoolOfRecord(uniqueSchoolList);
		LOGGER.info("unique schools =  {}", uniqueSchoolList.size());
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YED4".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,null);

			List<StudentCredentialDistribution> yed2List = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YED2".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",null);

			List<StudentCredentialDistribution> yedrList = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YEDR".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",null);

			List<StudentCredentialDistribution> yedbList = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YEDB".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",null);

			List<StudentCredentialDistribution> studentList = cList.stream().filter(scd->!"NONGRADDIST".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen())).collect(Collectors.toList());
			schoolDistributionPrintFile(studentList,batchId,usl,mapDist);
		});
		if (!cList.isEmpty()) {
			DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).activityCode(activityCode).studentSearchRequest(getStudentSearchRequest(searchRequest)).build();
			distributionRequest.setSchools(summaryDTO.getSchools());
			restUtils.mergeAndUpload(batchId, distributionRequest, activityCode, "N");
		}
	}

	protected void schoolDistributionPrintFile(List<StudentCredentialDistribution> studentList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
		if(!studentList.isEmpty()) {
			SchoolDistributionRequest tpReq = new SchoolDistributionRequest();
			tpReq.setBatchId(batchId);
			tpReq.setPsId(usl +" " +batchId);
			tpReq.setCount(studentList.size());
			tpReq.setStudentList(studentList);
			if(mapDist.get(usl) != null) {
				DistributionPrintRequest dist = mapDist.get(usl);
				dist.setSchoolDistributionRequest(tpReq);
				mapDist.put(usl,dist);
			} else {
				DistributionPrintRequest dist = new DistributionPrintRequest();
				dist.setSchoolDistributionRequest(tpReq);
				mapDist.put(usl,dist);
			}
		}
	}
}
