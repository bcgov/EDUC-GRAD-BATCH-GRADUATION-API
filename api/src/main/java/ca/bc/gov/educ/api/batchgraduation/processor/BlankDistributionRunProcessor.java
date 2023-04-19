package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.BlankDistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class BlankDistributionRunProcessor implements ItemProcessor<BlankCredentialDistribution, BlankCredentialDistribution> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BlankDistributionRunProcessor.class);

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	BlankDistributionSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;
    
	@Override
	public BlankCredentialDistribution process(BlankCredentialDistribution item) throws Exception {
		LOGGER.info("Processing partitionData = {}",item.getCredentialTypeCode());
		summaryDTO.setBatchId(batchId);
		return restUtils.processBlankDistribution(item, summaryDTO);
		
	}

    
}
