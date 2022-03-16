package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradAlgorithmService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class BatchPerformanceWriterTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";

    @Autowired
    private BatchPerformanceWriter batchPerformanceWriter;
    @MockBean
    GradAlgorithmService gradAlgorithmService;
    @MockBean
    RestUtils restUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testRetrieveSummaryDto() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        StepExecution stepExecution = new StepExecution("NoProcessingStep",new JobExecution(121L));
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setProcessedCount(10);
        summaryDTO.setErrors(new ArrayList<>());
        jobContext.put("summaryDTO",summaryDTO);
        summaryDTO = (AlgorithmSummaryDTO)jobContext.get("summaryDTO");
        summaryDTO.setBatchId(jobExecution.getId());
        jobContext.put("summaryDTO", summaryDTO);


        batchPerformanceWriter.retrieveSummaryDto(stepExecution);
        AlgorithmSummaryDTO summaryDTOss = (AlgorithmSummaryDTO)jobContext.get("summaryDTO");
        assertThat(summaryDTOss).isNotNull();
    }

    @Test
    public void testWrite() throws Exception {
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
        summaryDTO.setAccessToken("123");

        StepExecution stepExecution = new StepExecution("NoProcessingStep",new JobExecution(121L));
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();

        List<GraduationStudentRecord> list = new ArrayList<>();
        list.add(grd);

        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("123");
        obj.setRefresh_token("343");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        Mockito.when(restUtils.getStudentsForAlgorithm(summaryDTO.getAccessToken())).thenReturn(list);

        batchPerformanceWriter.write(list);
        assertThat(grd.getProgram()).isEqualTo("2018-EN");

    }
}
