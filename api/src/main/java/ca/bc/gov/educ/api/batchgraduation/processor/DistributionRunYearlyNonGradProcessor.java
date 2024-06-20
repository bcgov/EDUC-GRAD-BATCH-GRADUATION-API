package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class DistributionRunYearlyNonGradProcessor implements ItemProcessor<String, List<StudentCredentialDistribution>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public List<StudentCredentialDistribution> process(String mincode) throws Exception {
		summaryDTO.setBatchId(batchId);
		LOGGER.debug("Processing partitionData for district {} ", mincode);
		summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
		List<StudentCredentialDistribution> studentCredentials = restUtils.fetchDistributionRequiredDataStudentsNonGradYearly(mincode);
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		if(searchRequest != null && searchRequest.getPens() != null && !searchRequest.getPens().isEmpty()) {
			studentCredentials.removeIf(scr->!searchRequest.getPens().contains(scr.getPen()));
		}
		restUtils.deleteSchoolReportRecord(mincode, "NONGRADDISTREP_SC");
		restUtils.deleteSchoolReportRecord(mincode, "NONGRADDISTREP_SD");
		LOGGER.debug("Completed partitionData for district {} with {} students", mincode, studentCredentials.size());
		summaryDTO.getGlobalList().addAll(studentCredentials);
		return studentCredentials;
	}
}
