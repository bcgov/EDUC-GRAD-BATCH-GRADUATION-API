package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Ignore
public class RunProjectedGradAlgorithmProcessorTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";


    private RunProjectedGradAlgorithmProcessor runProjectedGradAlgorithmProcessor;

    @MockBean
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Mock
    protected StepExecution stepExecution;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testProcess() throws Exception {
        RunProjectedGradAlgorithmProcessor runProjectedGradAlgorithmProcessor = new RunProjectedGradAlgorithmProcessor();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        AlgorithmSummaryDTO summaryDTOs = new AlgorithmSummaryDTO();
        summaryDTOs.setReadCount(10L);
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = new ExecutionContext();
        jobContext.put("data", Arrays.asList(grd));
        jobContext.put("summary", summaryDTOs);
        stepExecution.setExecutionContext(jobContext);

        AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("summaryDTO");
        Mockito.when(restUtils.processProjectedGradStudent(grd, summaryDTO)).thenReturn(grd);

        runProjectedGradAlgorithmProcessor.process(grd);
        assertThat(grd.getProgram()).isEqualTo("2018-EN");

    }
}
