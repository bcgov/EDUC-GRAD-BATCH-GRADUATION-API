package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.EdwGraduationSnapshot;
import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSummaryDTO;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseSnapshotWriter implements ItemWriter<EdwGraduationSnapshot> {

    @Value("#{stepExecution.jobExecution.id}")
    Long batchId;

    @Value("#{stepExecutionContext['summary']}")
    EdwSnapshotSummaryDTO summaryDTO;

}
