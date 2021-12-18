package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class TvrRunBatchPerformanceWriter implements ItemWriter<GraduationStudentRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvrRunBatchPerformanceWriter.class);
    
    private AlgorithmSummaryDTO summaryDTO;
    
    @BeforeStep
    public void retrieveSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = (AlgorithmSummaryDTO)jobContext.get("summaryDTO");
    }
    
    @Override
    public void write(List<? extends GraduationStudentRecord> list) throws Exception {
        LOGGER.info("Recording Algorithm Processed Data");
        if(!list.isEmpty()) {
        	GraduationStudentRecord gradStatus = list.get(0);
	        summaryDTO.increment(gradStatus.getProgram());
	        LOGGER.info("Processed student[{}] - Student ID: {} in total {}", summaryDTO.getProcessedCount(), gradStatus.getStudentID(), summaryDTO.getReadCount());
            LOGGER.info("-------------------------------------------------------");
        }
    }

}
