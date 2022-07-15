package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolStudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentReportSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class StudentReportRunWriter implements ItemWriter<SchoolStudentCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentReportRunWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    StudentReportSummaryDTO summaryDTO;
    
    @Override
    public void write(List<? extends SchoolStudentCredentialDistribution> list) throws Exception {
        LOGGER.info("*** Recording School Report Posting Processed Data");
        if(!list.isEmpty()) {
        	SchoolStudentCredentialDistribution cred = list.get(0);
	        summaryDTO.increment(cred.getCredentialTypeCode());
            LOGGER.info("*** {} Partition * Number of Items Left : {}",Thread.currentThread().getName(),summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
            LOGGER.info("--------------------------------------------------------------------------------------------------------------------");
        }
    }

}
