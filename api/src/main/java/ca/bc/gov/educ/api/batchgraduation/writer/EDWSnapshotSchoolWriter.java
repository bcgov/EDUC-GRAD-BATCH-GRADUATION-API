package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSchoolSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.SnapshotResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class EDWSnapshotSchoolWriter implements ItemWriter<List<Pair<String, List<SnapshotResponse>>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotSchoolWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    EdwSnapshotSchoolSummaryDTO summaryDTO;

    @Override
    public void write(Chunk<? extends List<Pair<String, List<SnapshotResponse>>>> list) {
        if (!list.isEmpty()) {
            LOGGER.debug("List parameter size: {}", list.size());
            Pair<String, List<SnapshotResponse>> snapshot = list.getItems().get(0).get(0);
            String mincode = snapshot.getLeft();
            List<SnapshotResponse> snapshotList = snapshot.getRight();
            if (snapshotList != null && !snapshotList.isEmpty()) {
                LOGGER.debug("Mincode {}: Snapshots size: {}", mincode, snapshotList.size());
                summaryDTO.getGlobalList().addAll(snapshotList);
            }
        }
    }
}
