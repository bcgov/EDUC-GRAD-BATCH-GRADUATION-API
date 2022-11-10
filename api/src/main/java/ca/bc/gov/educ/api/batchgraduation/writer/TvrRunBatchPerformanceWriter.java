package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class TvrRunBatchPerformanceWriter implements ItemWriter<GraduationStudentRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvrRunBatchPerformanceWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;
    
    @Override
    public void write(List<? extends GraduationStudentRecord> list) throws Exception {
        if(!list.isEmpty()) {
        	GraduationStudentRecord gradStatus = list.get(0);
	        summaryDTO.increment(gradStatus.getProgram());
            LOGGER.info("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
