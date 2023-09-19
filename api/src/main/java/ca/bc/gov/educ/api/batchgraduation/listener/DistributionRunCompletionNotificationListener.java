package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortSchoolBySchoolOfRecord;
import static ca.bc.gov.educ.api.batchgraduation.util.GradSorter.sortStudentCredentialDistributionBySchoolAndNames;

@Component
public class DistributionRunCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunCompletionNotificationListener.class);

	@Autowired
	SupportListener supportListener;

    @Override
    public void afterJob(JobExecution jobExecution) {
		long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
		LOGGER.info("=======================================================================================");
		JobParameters jobParameters = jobExecution.getJobParameters();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		Long jobExecutionId = jobExecution.getId();
		String jobType = jobParameters.getString("jobType");
		LOGGER.info("{} Distribution Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis/1000, jobExecution.getStatus());

		String activityCode = "MONTHLYDIST";
		if(StringUtils.isNotBlank(jobType)) {
			switch (jobType) {
				case "DISTRUN" -> activityCode = "MONTHLYDIST";
				case "DISTRUN_YE" -> activityCode = "YEARENDDIST";
				case "DISTRUN_SUPP" -> activityCode = "SUPPDIST";
				case "NONGRADRUN" -> activityCode = "NONGRADYERUN";
				case "PSIRUN" -> activityCode = "PSIDIST";
			}
		}

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
		LOGGER.info(" --------------------------------------------------------------------------------------");
		DistributionSummaryDTO finalSummaryDTO = summaryDTO;
		summaryDTO.getCredentialCountMap().forEach((key, value) -> LOGGER.info(" {} count   : {}", key, finalSummaryDTO.getCredentialCountMap().get(key)));

		LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
		processGlobalList(summaryDTO,activityCode);
		LOGGER.info("=======================================================================================");
    }

	private void processGlobalList(DistributionSummaryDTO summaryDTO, String activityCode) {
		Long batchId = summaryDTO.getBatchId();
		List<StudentCredentialDistribution> cList = distributionService.getStudentCredentialDistributions(batchId);
		sortStudentCredentialDistributionBySchoolAndNames(cList);
		LOGGER.info("Student Credentials list size =  {}", cList.size());
		Map<String, DistributionPrintRequest> mapDist = summaryDTO.getMapDist();
		List<String> uniqueSchoolList = distributionService.getSchoolListForDistribution(batchId);
		sortSchoolBySchoolOfRecord(uniqueSchoolList);
		LOGGER.info("Unique Schools =  {}", uniqueSchoolList.size());
		uniqueSchoolList.forEach(usl->{
			List<StudentCredentialDistribution> yed4List = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YED4".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,null);

			List<StudentCredentialDistribution> yed2List = cList.stream().filter(scd->!"NONGRADYERUN".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YED2".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.certificatePrintFile(yed2List,batchId,usl,mapDist,"YED2",null);

			List<StudentCredentialDistribution> yedrList = cList.stream().filter(scd->!"NONGRADYERUN".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YEDR".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.certificatePrintFile(yedrList,batchId,usl,mapDist,"YEDR",null);

			List<StudentCredentialDistribution> yedbList = cList.stream().filter(scd->!"NONGRADYERUN".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && "YEDB".compareTo(scd.getPaperType()) == 0).collect(Collectors.toList());
			supportListener.certificatePrintFile(yedbList,batchId,usl,mapDist,"YEDB",null);

			List<StudentCredentialDistribution> studentList = cList.stream().filter(scd->!"NONGRADYERUN".equalsIgnoreCase(activityCode) && scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen())).collect(Collectors.toList());
			schoolDistributionPrintFile(studentList,batchId,usl,mapDist);
		});
		DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).activityCode(activityCode).build();
		distributionRequest.setSchools(new ArrayList<>());
		restUtils.mergeAndUpload(batchId, distributionRequest, activityCode, "N");
	}

	private void schoolDistributionPrintFile(List<StudentCredentialDistribution> studentList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
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
			}else{
				DistributionPrintRequest dist = new DistributionPrintRequest();
				dist.setSchoolDistributionRequest(tpReq);
				mapDist.put(usl,dist);
			}
		}
	}
}
