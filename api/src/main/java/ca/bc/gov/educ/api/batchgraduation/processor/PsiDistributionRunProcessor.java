package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.PsiDistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class PsiDistributionRunProcessor implements ItemProcessor<PsiCredentialDistribution, PsiCredentialDistribution> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PsiDistributionRunProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	PsiDistributionSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;
    
	@Override
	public PsiCredentialDistribution process(PsiCredentialDistribution item) throws Exception {
		LOGGER.info("Processing partitionData = {}", item.getPsiYear());
		summaryDTO.setBatchId(batchId);
		return restUtils.processPsiDistribution(item, summaryDTO);
		
	}

    
}
