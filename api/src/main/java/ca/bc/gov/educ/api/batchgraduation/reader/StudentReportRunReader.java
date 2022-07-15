package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolStudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class StudentReportRunReader extends StudentReportRunBaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentReportRunReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<SchoolStudentCredentialDistribution> credentialList;

    @Override
    public SchoolStudentCredentialDistribution read() throws Exception {
        LOGGER.info("*** Reading the information of the next Credential");

        if (nxtCredentialForProcessing % 300 == 0) {
            fetchAccessToken();
        }
        summaryDTO.setReadCount(credentialList.size());

        SchoolStudentCredentialDistribution nextCredential = null;
        
        if (nxtCredentialForProcessing < credentialList.size()) {
            nextCredential = credentialList.get(nxtCredentialForProcessing);
            LOGGER.info("*** Found Student [{}] - Student ID: {} in total {}", nxtCredentialForProcessing + 1, nextCredential.getStudentID(), summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
        }else {
        	aggregate("studentReportSummaryDTO");
        }
        return nextCredential;
    }
}
