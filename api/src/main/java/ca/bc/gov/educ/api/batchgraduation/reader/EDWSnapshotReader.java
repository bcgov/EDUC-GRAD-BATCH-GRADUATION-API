package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

public class EDWSnapshotReader extends BaseSchoolReader implements ItemReader<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotReader.class);

    @Override
    public String read() throws Exception {
        String nextSchool = null;
        if (nextSchoolForProcessing < schools.size()) {
            fetchAccessToken();
            nextSchool = schools.get(nextSchoolForProcessing);
            LOGGER.info("School Code:{} - {} of {}", nextSchool, nextSchoolForProcessing + 1, summaryDTO.getReadCount());
            nextSchoolForProcessing++;
        } else {
            aggregate("edwSnapshotSummaryDTO");
        }
        return nextSchool;
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
