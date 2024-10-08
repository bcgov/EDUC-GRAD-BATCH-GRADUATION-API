package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class RegenerateCertificateReader extends BaseReader implements ItemReader<StudentCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegenerateCertificateReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<StudentCredentialDistribution> credentialList;

    @Override
    public StudentCredentialDistribution read() throws Exception {
        summaryDTO.setReadCount(credentialList.size());

        StudentCredentialDistribution nextCredential = null;
        
        if (nxtCredentialForProcessing < credentialList.size()) {
            nextCredential = credentialList.get(nxtCredentialForProcessing);
            LOGGER.info("StudID:{} - {} of {}", nextCredential, nxtCredentialForProcessing + 1, summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
        } else {
            aggregate("regenCertSummaryDTO");
        }
        return nextCredential;
    }
}
