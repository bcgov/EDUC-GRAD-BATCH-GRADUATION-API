package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class RunProjectedGradAlgorithmProcessor implements ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunProjectedGradAlgorithmProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	AlgorithmSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;
    
	@Override
	public GraduationStudentRecord process(GraduationStudentRecord item) throws Exception {
		LOGGER.info("*** {} processing partitionData = {}",Thread.currentThread().getName(), item.getProgram());
		summaryDTO.setBatchId(batchId);
		return restUtils.processProjectedGradStudent(item, summaryDTO);
		
	}

    
}
