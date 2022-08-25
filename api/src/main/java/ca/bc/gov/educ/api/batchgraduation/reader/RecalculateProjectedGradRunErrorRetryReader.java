package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class RecalculateProjectedGradRunErrorRetryReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateProjectedGradRunErrorRetryReader.class);

    @Value("#{stepExecutionContext['index']}")
    private Integer nxtStudentForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<GraduationStudentRecord> studentList;

    @Override
    public GraduationStudentRecord read() throws Exception {
        LOGGER.info("*** Reading the information of the next student");

        if (nxtStudentForProcessing % 15 == 0) {
            fetchAccessToken();
        }
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
