package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public class DeleteStudentReportsReader implements ItemReader<List<UUID>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteStudentReportsReader.class);

    @Value("#{stepExecutionContext['data']}")
    List<UUID> guids;

    @Value("#{stepExecutionContext['readCount']}")
    Long readCount;

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Override
    public List<UUID> read() throws Exception {
        if(readCount > 0) {
            return null;
        }
        if(guids.isEmpty()) {
            readCount = summaryDTO.getReadCount();
        } else {
            readCount += guids.size();
        }
        summaryDTO.setReadCount(readCount);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.info("Read Student Guids -> {} students", readCount);
        }
        return guids;
    }
}
