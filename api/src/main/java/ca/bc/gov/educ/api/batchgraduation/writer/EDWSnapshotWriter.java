package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.EdwGraduationSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;

public class EDWSnapshotWriter extends BaseSnapshotWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotWriter.class);

    @Override
    public void write(Chunk<? extends EdwGraduationSnapshot> chunk) throws Exception {
        if (!chunk.isEmpty()) {
            LOGGER.debug("List parameter size: {}", chunk.size());
            EdwGraduationSnapshot snapshot = chunk.getItems().get(0);
            summaryDTO.increment(snapshot.getSchoolOfRecord());
        }
    }
}
