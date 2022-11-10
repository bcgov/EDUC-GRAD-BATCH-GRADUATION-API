package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RecalculateProjectedGradRunErrorReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateProjectedGradRunErrorReader.class);

    @Override
    public UUID read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(studentList.size());
        UUID nextStudent = null;
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("Error - StudID:{} - {} of {}", nextStudent, nxtStudentForProcessing + 1, summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }else {
            summaryDTO.setReadCount(0);
        	aggregate("tvrRunSummaryDTO");
        }
        return nextStudent;
    }
}
