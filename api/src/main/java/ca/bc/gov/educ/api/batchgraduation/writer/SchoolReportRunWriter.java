package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class SchoolReportRunWriter implements ItemWriter<SchoolReportDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolReportRunWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    SchoolReportSummaryDTO summaryDTO;
    
    @Override
    public void write(List<? extends SchoolReportDistribution> list) throws Exception {
        LOGGER.info("*** Recording School Report Posting Processed Data");
        if(!list.isEmpty()) {
        	SchoolReportDistribution cred = list.get(0);
	        summaryDTO.increment(cred.getReportTypeCode());
            LOGGER.info("*** {} Partition * Number of Items Left : {}",Thread.currentThread().getName(),summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
            LOGGER.info("--------------------------------------------------------------------------------------------------------------------");
        }
    }

}
