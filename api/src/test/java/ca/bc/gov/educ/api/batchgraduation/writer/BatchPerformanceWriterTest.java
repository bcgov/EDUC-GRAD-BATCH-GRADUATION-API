package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Ignore
public class BatchPerformanceWriterTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";

    private RegGradAlgBatchPerformanceWriter regGradAlgBatchPerformanceWriter;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testWrite() throws Exception {
        RegGradAlgBatchPerformanceWriter regGradAlgBatchPerformanceWriter = new RegGradAlgBatchPerformanceWriter();
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

        regGradAlgBatchPerformanceWriter.write(list);
        assertThat(grd.getProgram()).isEqualTo("2018-EN");

    }
}
