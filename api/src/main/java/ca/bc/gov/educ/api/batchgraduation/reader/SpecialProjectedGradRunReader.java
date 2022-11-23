package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SpecialProjectedGradRunReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialProjectedGradRunReader.class);

    @Override
    public UUID read() throws Exception {
        fetchAccessToken();
        UUID nextStudent = null;
        
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.debug("Stu[{}]-{}, total-{}", nxtStudentForProcessing + 1, nextStudent, summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }
        return nextStudent;
    }
}
