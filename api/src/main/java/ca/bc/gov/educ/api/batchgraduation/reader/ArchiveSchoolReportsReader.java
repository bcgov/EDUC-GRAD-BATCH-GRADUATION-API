package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class ArchiveSchoolReportsReader implements ItemReader<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveSchoolReportsReader.class);

    @Value("#{stepExecutionContext['data']}")
    List<String> schools;

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Value("#{stepExecutionContext['readCount']}")
    Long readCount;

    @Override
    public List<String> read() throws Exception {
        if(readCount > 0) return null;
        readCount++;
        if(LOGGER.isDebugEnabled()) {
            LOGGER.info("Read schools Codes -> {} of {} schools", schools.size(), String.join(",", schools));
        }
        return schools;
    }
}
