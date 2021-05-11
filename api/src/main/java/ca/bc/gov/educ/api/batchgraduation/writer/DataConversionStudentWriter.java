package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DataConversionStudentWriter implements ItemWriter<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionStudentWriter.class);

    private ConversionSummaryDTO summaryDTO;

    @BeforeStep
    public void retrieveSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = (ConversionSummaryDTO)jobContext.get("summaryDTO");
    }
    
    @Override
    public void write(List<? extends ConvGradStudent> list) throws Exception {
        if(list != null && list.size() > 0) {
            summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
            ConvGradStudent gradStudent = list.get(0);
            LOGGER.info("Processed student[{}] - PEN: {}  in total : {}", summaryDTO.getProcessedCount(), gradStudent.getPen());
        }
    }
}
