package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ProcessError;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class TvrRunJobCompletionNotificationListenerTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";

    @Autowired
    private TvrRunJobCompletionNotificationListener tvrRunJobCompletionNotificationListener;
    @MockBean BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;
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
    public void testAfterJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "TVRRUN");

        JobExecution ex = new JobExecution(121L);
        ex.setStatus(BatchStatus.COMPLETED);
        ex.setStartTime(new Date());
        ex.setEndTime(new Date());
        ex.setId(121L);
        ExecutionContext jobContext = new ExecutionContext();


        AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
        summaryDTO.setGlobalList(new ArrayList<>());
        summaryDTO.setProcessedCount(10);
        summaryDTO.setErrors(new HashMap<>());
        jobContext.put("summaryDTO", summaryDTO);

        JobParameters jobParameters = ex. getJobParameters();
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();
        String status = ex.getStatus().toString();
        Date startTime = ex.getStartTime();
        Date endTime = ex.getEndTime();
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(121L);
        ent.setStartTime(startTime);
        ent.setEndTime(endTime);
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);

        ex.setExecutionContext(jobContext);
        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        tvrRunJobCompletionNotificationListener.afterJob(ex);

        assertThat(ent.getActualStudentsProcessed()).isEqualTo(10);
    }

    @Test
    public void testAfterJob_witherror() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "TVRRUN");

        JobExecution ex = new JobExecution(121L);
        ex.setStatus(BatchStatus.COMPLETED);
        ex.setStartTime(new Date());
        ex.setEndTime(new Date());
        ex.setId(121L);
        ExecutionContext jobContext = new ExecutionContext();


        AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
        summaryDTO.setGlobalList(new ArrayList<>());
        summaryDTO.setProcessedCount(10);
        ProcessError er = new ProcessError();
        er.setReason("ERE");
        er.setDetail("erer");
        er.setStudentID(UUID.randomUUID().toString());
        Map<UUID,ProcessError> mapP = new HashMap<>();
        mapP.put(UUID.randomUUID(),er);
        summaryDTO.setErrors(mapP);
        jobContext.put("tvrRunSummaryDTO", summaryDTO);

        JobParameters jobParameters = ex. getJobParameters();
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();
        String status = ex.getStatus().toString();
        Date startTime = ex.getStartTime();
        Date endTime = ex.getEndTime();
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(121L);
        ent.setStartTime(startTime);
        ent.setEndTime(endTime);
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);

        ex.setExecutionContext(jobContext);
        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        tvrRunJobCompletionNotificationListener.afterJob(ex);

        assertThat(ent.getActualStudentsProcessed()).isEqualTo(10);
    }
}
