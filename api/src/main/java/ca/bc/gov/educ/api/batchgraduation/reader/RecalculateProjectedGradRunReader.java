package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RecalculateProjectedGradRunReader extends BaseStudentReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateProjectedGradRunReader.class);

    @Override
    public UUID read() throws Exception {
        fetchAccessToken();
        UUID nextStudent = null;

        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("StudID:{} - {} of {}", nextStudent, nxtStudentForProcessing + 1, summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }
        return nextStudent;
    }
}
