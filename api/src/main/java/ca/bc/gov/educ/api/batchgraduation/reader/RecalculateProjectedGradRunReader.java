package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import java.util.List;

public class RecalculateProjectedGradRunReader implements ItemReader<GraduationStudentRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateProjectedGradRunReader.class);

    private final RestUtils restUtils;

    private AlgorithmSummaryDTO summaryDTO;

    private int nxtStudentForProcessing;
    private List<GraduationStudentRecord> studentList;

    public RecalculateProjectedGradRunReader(RestUtils restUtils) {
        nxtStudentForProcessing = 0;
        this.restUtils = restUtils;
    }
    
    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new AlgorithmSummaryDTO();
        jobContext.put("summaryDTO", summaryDTO);
    }

    @Override
    public GraduationStudentRecord read() throws Exception {
        LOGGER.info("Reading the information of the next student");

        if (nxtStudentForProcessing % 10 == 0) {
            fetchAccessToken();
        }

        if (studentDataIsNotInitialized()) {
        	studentList = fetchStudentDataFromAPI();
        	summaryDTO.setReadCount(studentList.size());
        }

        GraduationStudentRecord nextStudent = null;
        
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("Found student[{}] - Student ID: {} in total {}", nxtStudentForProcessing + 1, nextStudent.getStudentID(), summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        }
        else {
        	nxtStudentForProcessing = 0;
            studentList = null;
        }
        return nextStudent;
    }

    private boolean studentDataIsNotInitialized() {
        return this.studentList == null;
    }

    private List<GraduationStudentRecord> fetchStudentDataFromAPI() {
        LOGGER.info("Fetching Student List that need Processing");
        fetchAccessToken();			
		return restUtils.getStudentsForProjectedAlgorithm(summaryDTO.getAccessToken());
    }
    
    private void fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
            LOGGER.info("Setting the new access token in summaryDTO.");
        }
    }
}
