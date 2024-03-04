package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;

public class TvrRunBatchPerformanceWriter extends BaseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvrRunBatchPerformanceWriter.class);

    @Override
    public void write(Chunk<? extends GraduationStudentRecord> list) throws Exception {
        if(!list.isEmpty()) {
        	list.forEach(this::saveBatchStatus);
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
