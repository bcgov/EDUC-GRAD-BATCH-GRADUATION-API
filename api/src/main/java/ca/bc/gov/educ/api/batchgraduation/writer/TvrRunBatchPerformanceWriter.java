package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;

public class TvrRunBatchPerformanceWriter extends BaseWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvrRunBatchPerformanceWriter.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    GradBatchHistoryService gradBatchHistoryService;
    
    @Override
    public void write(Chunk<? extends GraduationStudentRecord> list) throws Exception {
        if(!list.isEmpty()) {
        	GraduationStudentRecord gradStatus = list.getItems().get(0);
            saveBatchStatus(gradStatus);
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
