package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class SchoolReportRunProcessor implements ItemProcessor<SchoolReportDistribution, SchoolReportDistribution> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SchoolReportRunProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	SchoolReportSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;
    
	@Override
	public SchoolReportDistribution process(SchoolReportDistribution item) throws Exception {
		LOGGER.info("*** {} processing partitionData = {}",Thread.currentThread().getName(), item.getReportTypeCode());
		summaryDTO.setBatchId(batchId);
		return restUtils.processSchoolReportPosting(item, summaryDTO);
		
	}

    
}
