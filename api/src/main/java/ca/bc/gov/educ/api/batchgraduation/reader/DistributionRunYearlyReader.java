package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class DistributionRunYearlyReader extends DistributionRunBaseReader implements ItemReader<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<String> schoolsList;

    @Override
    public String read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(schoolsList.size());

        String nextSchool = null;
        
        if (nxtCredentialForProcessing < schoolsList.size()) {
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
