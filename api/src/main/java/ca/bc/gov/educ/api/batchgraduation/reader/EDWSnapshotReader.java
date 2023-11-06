package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.SnapshotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

public class EDWSnapshotReader extends BaseSnapshotReader implements ItemReader<SnapshotResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotReader.class);

    @Override
    public SnapshotResponse read() throws Exception {
        SnapshotResponse nextStudent = null;
        if (nextSnapshotForProcessing < snapshots.size()) {
            fetchAccessToken();
            nextStudent = snapshots.get(nextSnapshotForProcessing);
            LOGGER.info("Snapshot: pen# {} - {} of {}", nextStudent.getPen(), nextSnapshotForProcessing + 1, summaryDTO.getReadCount());
            nextSnapshotForProcessing++;
        } else {
            aggregate("edwSnapshotSummaryDTO");
        }
        return nextStudent;
    }

    private void aggregate(String summaryContextName) {
        EdwSnapshotSummaryDTO totalSummaryDTO = (EdwSnapshotSummaryDTO)jobExecution.getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new EdwSnapshotSummaryDTO();
            jobExecution.getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        totalSummaryDTO.setBatchId(summaryDTO.getBatchId());
        totalSummaryDTO.setGradYear(summaryDTO.getGradYear());
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getErrors().putAll(summaryDTO.getErrors());
        mergeMapCounts(totalSummaryDTO.getCountMap(),summaryDTO.getCountMap());
    }

}
