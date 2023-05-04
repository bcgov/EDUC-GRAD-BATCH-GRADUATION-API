package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class DistributionRunYearlyProcessor implements ItemProcessor<StudentCredentialDistribution, StudentCredentialDistribution> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public StudentCredentialDistribution process(StudentCredentialDistribution item) throws Exception {
		LOGGER.info("Processing partitionData = {}", item.getCredentialTypeCode());
		summaryDTO.setBatchId(batchId);
		return restUtils.processDistribution(item, summaryDTO);

	}
}
