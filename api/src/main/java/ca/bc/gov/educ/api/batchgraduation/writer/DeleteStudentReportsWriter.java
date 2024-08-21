package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public class DeleteStudentReportsWriter implements ItemWriter<List<UUID>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteStudentReportsWriter.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Value("#{stepExecutionContext['readCount']}")
    Long readCount;

    @Override
    public void write(Chunk<? extends List<UUID>> chunk) throws Exception {
        LOGGER.info("Delete Student Reports Writer");
        DistributionSummaryDTO distributionSummaryDTO = (DistributionSummaryDTO)jobExecution.getExecutionContext().get("distributionSummaryDTO");

        long readCount = distributionSummaryDTO.getReadCount();
        readCount += summaryDTO.getReadCount();
        distributionSummaryDTO.setReadCount(readCount);

        long processedCount = distributionSummaryDTO.getProcessedCount();
        processedCount += summaryDTO.getProcessedCount();
        distributionSummaryDTO.setProcessedCount(processedCount);

        distributionSummaryDTO.getSchools().addAll(summaryDTO.getSchools());
    }
}
