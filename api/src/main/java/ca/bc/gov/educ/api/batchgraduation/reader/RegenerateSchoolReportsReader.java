package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
public class RegenerateSchoolReportsReader implements ItemReader<List<String>> {

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
        if(log.isDebugEnabled()) {
            log.info("Read schools Codes -> {} of {} schools", readCount, schools.size());
        }
        return schools;
    }
}
