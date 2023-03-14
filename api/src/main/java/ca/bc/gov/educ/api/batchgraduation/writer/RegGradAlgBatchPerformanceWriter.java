package ca.bc.gov.educ.api.batchgraduation.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;

public class RegGradAlgBatchPerformanceWriter extends BaseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegGradAlgBatchPerformanceWriter.class);

    @Override
    public void write(List<? extends GraduationStudentRecord> list) throws Exception {
        if(!list.isEmpty()) {
        	GraduationStudentRecord gradStatus = list.get(0);
            saveBatchStatus(gradStatus);
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
