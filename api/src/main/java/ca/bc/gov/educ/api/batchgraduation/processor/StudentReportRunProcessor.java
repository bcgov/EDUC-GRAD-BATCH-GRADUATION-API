package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolStudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentReportSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class StudentReportRunProcessor implements ItemProcessor<SchoolStudentCredentialDistribution, SchoolStudentCredentialDistribution> {

	private static final Logger LOGGER = LoggerFactory.getLogger(StudentReportRunProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	StudentReportSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;
    
	@Override
	public SchoolStudentCredentialDistribution process(SchoolStudentCredentialDistribution item) throws Exception {
		LOGGER.info("*** {} processing partitionData = {}",Thread.currentThread().getName(), item.getCredentialTypeCode());
		summaryDTO.setBatchId(batchId);
		return restUtils.processStudentReportPosting(item, summaryDTO);
		
	}

    
}
