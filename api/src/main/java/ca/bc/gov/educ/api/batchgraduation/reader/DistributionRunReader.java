package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class DistributionRunReader extends DistributionRunBaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtCredentialForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<StudentCredentialDistribution> credentialList;

    @Override
    public StudentCredentialDistribution read() throws Exception {
        LOGGER.info("*** Reading the information of the next Credential");

        if (nxtCredentialForProcessing % 300 == 0) {
            fetchAccessToken();
        }
        summaryDTO.setReadCount(credentialList.size());

        StudentCredentialDistribution nextCredential = null;
        
        if (nxtCredentialForProcessing < credentialList.size()) {
            nextCredential = credentialList.get(nxtCredentialForProcessing);
            LOGGER.info("*** Found Credential[{}] - Student ID: {} in total {}", nxtCredentialForProcessing + 1, nextCredential.getStudentID(), summaryDTO.getReadCount());
            nxtCredentialForProcessing++;
        }else {
        	aggregate("distributionSummaryDTO");
        }
        return nextCredential;
    }
}