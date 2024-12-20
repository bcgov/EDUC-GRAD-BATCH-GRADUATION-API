package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSchoolSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

public class EDWSnapshotSchoolReader extends BaseMinCodeReader implements ItemReader<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotSchoolReader.class);

    @Value("#{stepExecutionContext['summary']}")
    EdwSnapshotSchoolSummaryDTO summaryDTO;

    @Override
    public String read() throws Exception {
        String nextSchool = null;
        if (nextSchoolForProcessing < schools.size()) {
            nextSchool = schools.get(nextSchoolForProcessing);
            LOGGER.info("School: {} - {} of {}", nextSchool, nextSchoolForProcessing + 1, summaryDTO.getReadCount());
            nextSchoolForProcessing++;
        } else {
            aggregate("edwSnapshotSchoolSummaryDTO");
        }
        return nextSchool;
    }

    private void aggregate(String summaryContextName) {
        EdwSnapshotSchoolSummaryDTO totalSummaryDTO = (EdwSnapshotSchoolSummaryDTO)jobExecution.getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new EdwSnapshotSchoolSummaryDTO();
            jobExecution.getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        totalSummaryDTO.setBatchId(summaryDTO.getBatchId());
        totalSummaryDTO.setGradYear(summaryDTO.getGradYear());
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.getGlobalList().addAll(summaryDTO.getGlobalList());
    }

}
