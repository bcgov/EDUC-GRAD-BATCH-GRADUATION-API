package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;

public abstract class BasePartitioner extends SimplePartitioner {
    protected String stepType;

    protected abstract JobExecution getJobExecution();

    protected void initializeTotalSummaryDTO(String summaryContextName, long readCount, boolean isRetry) {
        AlgorithmSummaryDTO totalSummaryDTO = (AlgorithmSummaryDTO)getJobExecution().getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new AlgorithmSummaryDTO();
            totalSummaryDTO.initializeProgramCountMap();
            getJobExecution().getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        if (!isRetry) {
            totalSummaryDTO.setTotalReadCount(readCount);
        }
    }
}
