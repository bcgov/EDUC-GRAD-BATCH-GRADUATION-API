package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class DistributionRunYearlyNonGradByMincodeProcessor implements ItemProcessor<String, List<StudentCredentialDistribution>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradByMincodeProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public List<StudentCredentialDistribution> process(String mincode) throws Exception {
		LOGGER.debug("Processing partitionData = {}", mincode);
		summaryDTO.setBatchId(batchId);
		summaryDTO.setProcessedCyclesCount(summaryDTO.getProcessedCyclesCount() + 1);
		summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
		List<StudentCredentialDistribution> studentCredentials = restUtils.fetchDistributionRequiredDataStudentsNonGradYearly(mincode);
		LOGGER.debug("Completed partitionData = {}", studentCredentials.size());
		summaryDTO.getGlobalList().addAll(studentCredentials);
		return summaryDTO.getGlobalList();
	}
}
