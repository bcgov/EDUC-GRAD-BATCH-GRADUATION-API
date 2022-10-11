package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class BlankDistributionRunReader extends BlankDistributionRunBaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlankDistributionRunReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<BlankCredentialDistribution> credentialList;

    @Override
    public BlankCredentialDistribution read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(credentialList.size());

        BlankCredentialDistribution nextCredential = null;
        
        if (nxtCredentialForProcessing < credentialList.size()) {
            nextCredential = credentialList.get(nxtCredentialForProcessing);
            LOGGER.info("*** Found Credential[{}]- in total {}", nxtCredentialForProcessing + 1, summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
        }else {
        	aggregate("blankDistributionSummaryDTO");
        }
        return nextCredential;
    }
}
