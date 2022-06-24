package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class SchoolReportRunReader extends SchoolReportRunBaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolReportRunReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<SchoolReportDistribution> credentialList;

    @Override
    public SchoolReportDistribution read() throws Exception {
        LOGGER.info("*** Reading the information of the next Credential");

        if (nxtCredentialForProcessing % 300 == 0) {
            fetchAccessToken();
        }
        summaryDTO.setReadCount(credentialList.size());

        SchoolReportDistribution nextCredential = null;
        
        if (nxtCredentialForProcessing < credentialList.size()) {
            nextCredential = credentialList.get(nxtCredentialForProcessing);
            LOGGER.info("*** Found Report Type [{}] - School ID: {} in total {}", nxtCredentialForProcessing + 1, nextCredential.getSchoolOfRecord(), summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
        }else {
        	aggregate("schoolReportSummaryDTO");
        }
        return nextCredential;
    }
}
