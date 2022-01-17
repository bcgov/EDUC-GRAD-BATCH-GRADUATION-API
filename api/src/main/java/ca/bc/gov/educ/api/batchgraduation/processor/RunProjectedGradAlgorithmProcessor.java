package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.service.GradAlgorithmService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class RunProjectedGradAlgorithmProcessor implements ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> {

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
		summaryDTO.setBatchId(jobExecution.getId());
	}
    
	@Override
	public GraduationStudentRecord process(GraduationStudentRecord item) throws Exception {
		return gradAlgorithmService.processProjectedGradStudent(item, summaryDTO);
		
	}

    
}