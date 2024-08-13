package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.BlankDistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.USER_SCHEDULED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class UserReqBlankDistributionRunCompletionNotificationListenerTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";

    @Mock
    WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock WebClient.ResponseSpec responseMock;
    @Mock WebClient.RequestBodySpec requestBodyMock;
    @Mock WebClient.RequestBodyUriSpec requestBodyUriMock;

    @Autowired
    private UserReqBlankDistributionRunCompletionNotificationListener userReqBlankDistributionRunCompletionNotificationListener;
    @MockBean BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;
    @MockBean
    RestUtils restUtils;

    @Autowired
    EducGradBatchGraduationApiConstants constants;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Autowired
    GraduationReportService graduationReportService;

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
        builder.addString("credentialType","OT");
        builder.addString(USER_SCHEDULED, UUID.randomUUID().toString());

        JobExecution jobExecution = new JobExecution(new JobInstance(121L,"UserReqDistributionBatchJob"), 121L, builder.toJobParameters());
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now());
        jobExecution.setId(121L);
        ExecutionContext jobContext = jobExecution.getExecutionContext();

        List<BlankCredentialDistribution> scdList = new ArrayList<>();
        BlankCredentialDistribution scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("E");
        scd.setPaperType("YED2");
        scd.setSchoolOfRecord("05005001");
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("EI");
        scd.setPaperType("YEDR");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("S");
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("S");
        scd.setPaperType("YEDB");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("X");
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);

        BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
        summaryDTO.setProcessedCount(10);
        summaryDTO.setErrors(new ArrayList<>());
        summaryDTO.setGlobalList(scdList);
        jobContext.put("blankDistributionSummaryDTO", summaryDTO);

        JobParameters jobParameters = jobExecution. getJobParameters();
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();
        String status = jobExecution.getStatus().toString();
        Date startTime = DateUtils.toDate(jobExecution.getStartTime());
        Date endTime = DateUtils.toDate(jobExecution.getEndTime());
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");
        String credentialType = jobParameters.getString("credentialType");

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(121L);
        ent.setStartTime(DateUtils.toLocalDateTime(startTime));
        ent.setEndTime(DateUtils.toLocalDateTime(endTime));
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);

        jobExecution.setExecutionContext(jobContext);

        List<BlankCredentialDistribution> cList = new ArrayList<>();
        cList.add(scd);

        List<BlankCredentialDistribution> tList = new ArrayList<>();
        tList.add(scd);

        ParameterizedTypeReference<List<StudentCredentialDistribution>> cListRes = new ParameterizedTypeReference<>() {
        };

        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        userReqBlankDistributionRunCompletionNotificationListener.afterJob(jobExecution);

        assertThat(ent.getActualStudentsProcessed()).isEqualTo(10);
    }

    @Test
    public void testAfterJob_OC() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "TVRRUN");
        builder.addString("credentialType","OC");

        JobExecution jobExecution = new JobExecution(new JobInstance(121L,"UserReqDistributionBatchJob"), 121L, builder.toJobParameters());
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now());
        jobExecution.setId(121L);
        ExecutionContext jobContext = jobExecution.getExecutionContext();

        List<BlankCredentialDistribution> scdList = new ArrayList<>();
        BlankCredentialDistribution scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("E");
        scd.setPaperType("YED2");
        scd.setSchoolOfRecord("05005001");
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("EI");
        scd.setPaperType("YEDR");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("S");
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("X");
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new BlankCredentialDistribution();
        scd.setCredentialTypeCode("S");
        scd.setPaperType("YEDB");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);

        BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
        summaryDTO.setProcessedCount(10);
        summaryDTO.setErrors(new ArrayList<>());
        summaryDTO.setGlobalList(scdList);
        jobContext.put("blankDistributionSummaryDTO", summaryDTO);

        JobParameters jobParameters = jobExecution. getJobParameters();
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();
        String status = jobExecution.getStatus().toString();
        Date startTime = DateUtils.toDate(jobExecution.getStartTime());
        Date endTime = DateUtils.toDate(jobExecution.getEndTime());
        String jobTrigger = jobParameters.getString("jobTrigger");
        String jobType = jobParameters.getString("jobType");
        String credentialType = jobParameters.getString("credentialType");

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(121L);
        ent.setStartTime(DateUtils.toLocalDateTime(startTime));
        ent.setEndTime(DateUtils.toLocalDateTime(endTime));
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);

        jobExecution.setExecutionContext(jobContext);

        List<BlankCredentialDistribution> cList = new ArrayList<>();
        cList.add(scd);

        List<BlankCredentialDistribution> tList = new ArrayList<>();
        tList.add(scd);

        ParameterizedTypeReference<List<StudentCredentialDistribution>> cListRes = new ParameterizedTypeReference<>() {
        };

        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        userReqBlankDistributionRunCompletionNotificationListener.afterJob(jobExecution);

        assertThat(ent.getActualStudentsProcessed()).isEqualTo(10);
    }
}
