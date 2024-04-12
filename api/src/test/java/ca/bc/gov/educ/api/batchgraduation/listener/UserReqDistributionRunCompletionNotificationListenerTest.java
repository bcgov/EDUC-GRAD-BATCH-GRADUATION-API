package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class UserReqDistributionRunCompletionNotificationListenerTest {

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
    private UserReqDistributionRunCompletionNotificationListener userReqDistributionRunCompletionNotificationListener;
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
        builder.addString("userScheduled", UUID.randomUUID().toString());

        JobExecution jobExecution = new JobExecution(new JobInstance(121L,"UserReqDistributionBatchJob"), 121L, builder.toJobParameters());
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now());
        jobExecution.setId(121L);
        ExecutionContext jobContext = jobExecution.getExecutionContext();

        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("E");
        scd.setPaperType("YED2");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("BC1996-PUB");
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("S");
        scd.setPaperType("YEDB");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("X");
        scd.setPaperType("YEDR");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
        summaryDTO.setCredentialType("OT");
        summaryDTO.setProcessedCount(10);
        summaryDTO.setErrors(new ArrayList<>());
        summaryDTO.setGlobalList(scdList);
        jobContext.put("distributionSummaryDTO", summaryDTO);

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

        List<StudentCredentialDistribution> cList = new ArrayList<>();
        cList.add(scd);

        List<StudentCredentialDistribution> tList = new ArrayList<>();
        tList.add(scd);

        DistributionDataParallelDTO dp = new DistributionDataParallelDTO(tList,cList);

        ParameterizedTypeReference<List<StudentCredentialDistribution>> tListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(tList));

        ParameterizedTypeReference<List<StudentCredentialDistribution>> cListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getCertificateDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(cListRes)).thenReturn(Mono.just(cList));

        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        Mockito.when(graduationReportService.getTranscriptList(null)).thenReturn(Mono.just(tList));
        Mockito.when(graduationReportService.getCertificateList(null)).thenReturn(Mono.just(cList));
        Mockito.when(parallelDataFetch.fetchDistributionRequiredData()).thenReturn(Mono.just(dp));
        userReqDistributionRunCompletionNotificationListener.afterJob(jobExecution);

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

        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("E");
        scd.setPaperType("YED2");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("BC1996-PUB");
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("S");
        scd.setPaperType("YEDB");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);
        scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("X");
        scd.setPaperType("YEDR");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
        summaryDTO.setCredentialType("OT");
        summaryDTO.setProcessedCount(10);
        summaryDTO.setErrors(new ArrayList<>());
        summaryDTO.setGlobalList(scdList);
        jobContext.put("distributionSummaryDTO", summaryDTO);

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

        List<StudentCredentialDistribution> cList = new ArrayList<>();
        cList.add(scd);

        List<StudentCredentialDistribution> tList = new ArrayList<>();
        tList.add(scd);

        DistributionDataParallelDTO dp = new DistributionDataParallelDTO(tList,cList);

        ParameterizedTypeReference<List<StudentCredentialDistribution>> tListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptYearlyDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(tList));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(tList));

        ParameterizedTypeReference<List<StudentCredentialDistribution>> cListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getCertificateDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(cListRes)).thenReturn(Mono.just(cList));

        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();
        reportGradStudentData.setGraduationStudentRecordId(scd.getStudentID());
        reportGradStudentData.setFirstName(scd.getLegalFirstName());
        reportGradStudentData.setLastName(scd.getLegalLastName());

        ParameterizedTypeReference<List<ReportGradStudentData>> repListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getStudentDataNonGradEarlyByMincode())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(repListRes)).thenReturn(Mono.just(List.of(reportGradStudentData)));

        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        Mockito.when(graduationReportService.getTranscriptList(null)).thenReturn(Mono.just(tList));
        Mockito.when(graduationReportService.getCertificateList(null)).thenReturn(Mono.just(cList));
        Mockito.when(parallelDataFetch.fetchDistributionRequiredData()).thenReturn(Mono.just(dp));
        Mockito.when(parallelDataFetch.fetchDistributionRequiredDataYearly()).thenReturn(Mono.just(dp));
        userReqDistributionRunCompletionNotificationListener.afterJob(jobExecution);

        assertThat(ent.getActualStudentsProcessed()).isEqualTo(10);
    }
}
