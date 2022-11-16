package ca.bc.gov.educ.api.batchgraduation.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.springframework.beans.factory.annotation.Value;

public class RegGradAlgBatchPerformanceWriter implements ItemWriter<GraduationStudentRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegGradAlgBatchPerformanceWriter.class);

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
