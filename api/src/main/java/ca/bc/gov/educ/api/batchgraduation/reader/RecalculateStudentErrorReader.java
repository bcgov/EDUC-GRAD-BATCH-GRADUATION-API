package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecalculateStudentErrorReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateStudentErrorReader.class);

    @Override
    public GraduationStudentRecord read() throws Exception {
        LOGGER.info("*** Reading the information of the next student");

        if (nxtStudentForProcessing % 15 == 0) {
            fetchAccessToken();
        }
        GraduationStudentRecord nextStudent = null;
        summaryDTO.setReadCount(0);
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("***  Error Found student[{}] - Student ID: {} in total {}", nxtStudentForProcessing + 1, nextStudent.getStudentID(), summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }else {
        	aggregate("regGradAlgSummaryDTO");
        }
        return nextStudent;
    }
}
