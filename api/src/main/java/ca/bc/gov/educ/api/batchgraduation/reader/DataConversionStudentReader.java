package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.service.DataConversionService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;
import ca.bc.gov.educ.api.batchgraduation.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

public class DataConversionStudentReader implements ItemReader<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionStudentReader.class);

    private final DataConversionService dataConversionService;
    private final RestUtils restUtils;

    private int indexForStudent;
    private List<ConvGradStudent> studentList;
    private ConversionSummaryDTO summaryDTO;

    public DataConversionStudentReader(DataConversionService dataConversionService, RestUtils restUtils) {
        this.dataConversionService = dataConversionService;
        this.restUtils = restUtils;

        indexForStudent = 0;
    }

    @BeforeStep
    public void initializeSummaryDto(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        summaryDTO = new ConversionSummaryDTO();
        summaryDTO.setTableName("GRAD_STUDENT");
        jobContext.put("summaryDTO", summaryDTO);
    }

    @Override
    public ConvGradStudent read() {
        LOGGER.info("Reading the information of the next student");

        if (indexForStudent % 300 == 0) {
            fetchAccessToken();
        }

        if (studentDataIsNotInitialized()) {
        	studentList = loadRawStudentData();
        	summaryDTO.setReadCount(studentList.size());
        }

        ConvGradStudent nextStudent = null;
        
        if (indexForStudent < studentList.size()) {
            nextStudent = studentList.get(indexForStudent);
            LOGGER.info("Found student[{}] - PEN: {} in total {}", indexForStudent + 1, nextStudent.getPen(), summaryDTO.getReadCount());
            indexForStudent++;
        }
        else {
        	indexForStudent = 0;
            studentList = null;
        }
        return nextStudent;
    }

    private boolean studentDataIsNotInitialized() {
        return this.studentList == null;
    }

    private List<ConvGradStudent> loadRawStudentData() {
        LOGGER.info("Fetching Student List that need Data Conversion Processing");
        return dataConversionService.loadInitialRawGradStudentData(false);
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
