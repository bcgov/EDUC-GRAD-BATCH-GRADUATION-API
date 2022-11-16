package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class PsiDistributionRunReader extends PsiDistributionRunBaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PsiDistributionRunReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<PsiCredentialDistribution> credentialList;

    @Override
    public PsiCredentialDistribution read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(credentialList.size());

        PsiCredentialDistribution nextCredential = null;
        
        if (nxtCredentialForProcessing < credentialList.size()) {
            nextCredential = credentialList.get(nxtCredentialForProcessing);
            LOGGER.debug("Cred[{}]-StuID:{} total-{}", nxtCredentialForProcessing + 1, nextCredential.getPen(), summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
        }else {
        	aggregate("psiDistributionSummaryDTO");
        }
        return nextCredential;
    }
}
