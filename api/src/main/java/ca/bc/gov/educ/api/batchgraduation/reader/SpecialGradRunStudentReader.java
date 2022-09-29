package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecialGradRunStudentReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialGradRunStudentReader.class);

    @Override
    public GraduationStudentRecord read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(studentList.size());
        GraduationStudentRecord nextStudent = null;
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("*** Found student[{}] - Student ID: {} in total {}", nxtStudentForProcessing + 1, nextStudent.getStudentID(), summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }else {
        	aggregate("spcRunAlgSummaryDTO");
        }
        return nextStudent;
    }
}
