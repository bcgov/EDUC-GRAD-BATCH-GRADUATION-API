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

	@Value("#{stepExecutionContext['searchRequestObject']}")
	StudentSearchRequest searchRequest;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public List<StudentCredentialDistribution> process(String mincode) throws Exception {
		summaryDTO.setBatchId(batchId);
		if(searchRequest != null && searchRequest.getDistricts() != null && !searchRequest.getDistricts().isEmpty() && searchRequest.getDistricts().contains(mincode)) {
			LOGGER.debug("Processing partitionData for district {} ", mincode);
			summaryDTO.setProcessedCyclesCount(summaryDTO.getProcessedCyclesCount() + 1);
			summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
			List<StudentCredentialDistribution> studentCredentials = restUtils.fetchDistributionRequiredDataStudentsNonGradYearly(mincode);
			LOGGER.debug("Completed partitionData for district {} with {} students", mincode, studentCredentials.size());
			summaryDTO.getGlobalList().addAll(studentCredentials);
		} else {
			LOGGER.debug("Skip partitionData for district {} due to filter {}", mincode, String.join(",", searchRequest.getDistricts()));
		}
		return summaryDTO.getGlobalList();
	}
}