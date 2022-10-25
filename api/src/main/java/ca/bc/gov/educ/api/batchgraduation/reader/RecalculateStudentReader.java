package ca.bc.gov.educ.api.batchgraduation.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RecalculateStudentReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateStudentReader.class);

    @Override
    public UUID read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(studentList.size());

        UUID nextStudent = null;
        
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("*** Found student[{}] - Student ID: {} in total {}", nxtStudentForProcessing + 1, nextStudent, summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }else {
        	aggregate("regGradAlgSummaryDTO");
        }
        return nextStudent;
    }
}
