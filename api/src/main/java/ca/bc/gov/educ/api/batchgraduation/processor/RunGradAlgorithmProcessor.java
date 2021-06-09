package ca.bc.gov.educ.api.batchgraduation.processor;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.service.GradAlgorithmService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;

public class RunGradAlgorithmProcessor implements ItemProcessor<GraduationStatus,GraduationStatus> {

	@Autowired
	EducGradBatchGraduationApiConstants constants;
	
	@Autowired
	private GradAlgorithmService gradAlgorithmService;

	private AlgorithmSummaryDTO summaryDTO;

	@BeforeStep
	public void retrieveSummaryDto(StepExecution stepExecution) {
		JobExecution jobExecution = stepExecution.getJobExecution();
		ExecutionContext jobContext = jobExecution.getExecutionContext();
		summaryDTO = (AlgorithmSummaryDTO)jobContext.get("summaryDTO");
	}
    
	@Override
	public GraduationStatus process(GraduationStatus item) throws Exception {
		return gradAlgorithmService.processStudent(item, summaryDTO);
		
	}

    
}
