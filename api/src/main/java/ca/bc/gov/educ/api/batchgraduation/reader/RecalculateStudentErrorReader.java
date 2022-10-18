package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RecalculateStudentErrorReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateStudentErrorReader.class);

    @Override
    public UUID read() throws Exception {
        fetchAccessToken();
        UUID nextStudent = null;
        summaryDTO.setReadCount(studentList.size());
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("***  Error Found student[{}] - Student ID: {} in total {}", nxtStudentForProcessing + 1, nextStudent, summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }else {
            summaryDTO.setReadCount(0);
        	aggregate("regGradAlgSummaryDTO");
        }
        return nextStudent;
    }
}
