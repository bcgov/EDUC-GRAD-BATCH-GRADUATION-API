package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public class ArchiveSchoolReportsReader implements ItemReader<List<UUID>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveSchoolReportsReader.class);

    @Value("#{stepExecutionContext['data']}")
    List<UUID> schools;

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Value("#{stepExecutionContext['readCount']}")
    Long readCount;

    @Override
    public List<UUID> read() throws Exception {
        if(readCount > 0) return null;
        readCount++;
        if(LOGGER.isDebugEnabled()) {
            LOGGER.info("Read schools Codes -> {} of {} schools", schools.size(), String.join(",", schools.toString()));
        }
        return schools;
    }
}
