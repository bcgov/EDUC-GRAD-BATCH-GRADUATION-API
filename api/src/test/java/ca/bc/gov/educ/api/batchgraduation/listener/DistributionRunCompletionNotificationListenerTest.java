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
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DistributionRunCompletionNotificationListenerTest {

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
    private DistributionRunCompletionNotificationListener distributionRunCompletionNotificationListener;
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

        JobExecution jobExecution = new JobExecution(121L);
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now());
        jobExecution.setId(121L);
        ExecutionContext jobContext = new ExecutionContext();


        Map<String,DistributionPrintRequest> mapDist = new HashMap<>();
        DistributionPrintRequest dpR = new DistributionPrintRequest();

        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("E");
        scd.setPaperType("YED2");
        scd.setSchoolOfRecord("05005001");
        scdList.add(scd);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setAccessToken("123");
        summaryDTO.setBatchId(121L);
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
        TranscriptPrintRequest tr = new TranscriptPrintRequest();
        tr.setTranscriptList(tList);
        CertificatePrintRequest cr = new CertificatePrintRequest();
        cr.setCertificateList(cList);
        cr.setPsId("05005001 121");
        cr.setCount(1);
        cr.setBatchId(121L);
        dpR.setYed2CertificatePrintRequest(cr);
        dpR.setTotal(1);
        SchoolDistributionRequest schoolDistributionRequest = new SchoolDistributionRequest();
        schoolDistributionRequest.setBatchId(121L);
        schoolDistributionRequest.setPsId("05005001 121");
        schoolDistributionRequest.setCount(1);
        schoolDistributionRequest.setStudentList(scdList);
        dpR.setSchoolDistributionRequest(schoolDistributionRequest);

        mapDist.put("05005001",dpR);
        DistributionDataParallelDTO dp = new DistributionDataParallelDTO(tList,cList);

        ParameterizedTypeReference<List<StudentCredentialDistribution>> tListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(tList));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptYearlyDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(tList));

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

        ParameterizedTypeReference<List<StudentCredentialDistribution>> cListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getCertificateDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(cListRes)).thenReturn(Mono.just(cList));

        ResponseObj obj = new ResponseObj();
        obj.setAccess_token("asdasd");

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).activityCode("YEARENDDIST").build();
        Mockito.when(restUtils.getTokenResponseObject()).thenReturn(obj);
        Mockito.when(graduationReportService.getTranscriptList(null)).thenReturn(Mono.just(tList));
        Mockito.when(graduationReportService.getCertificateList(null)).thenReturn(Mono.just(cList));
        Mockito.when(parallelDataFetch.fetchDistributionRequiredData()).thenReturn(Mono.just(dp));
        Mockito.when(parallelDataFetch.fetchDistributionRequiredDataYearly()).thenReturn(Mono.just(dp));
        Mockito.when(restUtils.mergeAndUpload(121L, distributionRequest,"YEARENDDIST",null)).thenReturn(new DistributionResponse());
        distributionRunCompletionNotificationListener.afterJob(jobExecution);

        assertThat(ent.getActualStudentsProcessed()).isEqualTo(10);
    }
}
