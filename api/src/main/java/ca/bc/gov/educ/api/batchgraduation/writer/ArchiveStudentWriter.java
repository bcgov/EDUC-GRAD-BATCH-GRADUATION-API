package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;

public class ArchiveStudentWriter extends BaseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentWriter.class);

    @Override
    public void write(Chunk<? extends GraduationStudentRecord> list) {
        if(!list.isEmpty()) {
        	GraduationStudentRecord gradStatus = list.getItems().get(0);
            saveBatchStatus(gradStatus);
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
