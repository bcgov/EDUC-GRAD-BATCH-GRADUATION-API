package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecalculateProjectedGradRunErrorRetryReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateProjectedGradRunErrorRetryReader.class);

    @Override
    public GraduationStudentRecord read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(0);
        GraduationStudentRecord nextStudent = null;
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("*** Error Found student[{}] - Student ID: {} in total {}", nxtStudentForProcessing + 1, nextStudent.getStudentID(), summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }else {
        	aggregate("tvrRunSummaryDTO");
        }
        return nextStudent;
    }
}
