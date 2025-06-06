package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public class DistributionRunYearlyNonGradReader extends DistributionRunBaseReader implements ItemReader<UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<UUID> schoolsList;

    @Override
    public UUID read() throws Exception {
        UUID nextSchool = null;
        if (nxtCredentialForProcessing < schoolsList.size()) {
            summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1);
            nextSchool = schoolsList.get(nxtCredentialForProcessing);
            LOGGER.info("School Code:{} - {} of {}", nextSchool, nxtCredentialForProcessing + 1, summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
            summaryDTO.getMapDist().clear();
        } else {
        	aggregate("distributionSummaryDTO");
        }
        return nextSchool;
    }
}
