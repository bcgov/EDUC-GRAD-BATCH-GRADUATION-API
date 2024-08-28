package ca.bc.gov.educ.api.batchgraduation.util;


import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import lombok.val;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RestUtilsTest {

    @Autowired
    GraduationReportService graduationReportService;

    @Autowired
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    private EducGradBatchGraduationApiConstants constants;

    @Mock
    private Mono<GradCertificateTypes> inputResponse;
    @Mock
    private Mono<GraduationStudentRecordDistribution> inputResponseGSR;
    @Mock
    private Mono<GraduationStudentRecordSearchResult> inputResponseSR;

    @Mock
    private Mono<Boolean> inputResponseBoolean;

    @Mock
    private Mono<DistributionResponse> inputResponsePSI;

    @Mock
    private Mono<Integer> inputResponseI;

    @Mock
    private Retry retryMock;

    @Mock
    private RetryBackoffSpec retryBackoffSpecMock;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetTokenResponseObject_returnsToken_with_APICallSuccess() {
        String mockToken = mockTokenResponseObject();

        val result = this.restUtils.getTokenResponseObject();
        assertThat(result).isNotNull();
        assertThat(result.getAccess_token()).isEqualTo(mockToken);
        assertThat(result.getRefresh_token()).isEqualTo("456");
    }

    @Test
    public void testGetStudentByPen_givenValues_returnsStudent_with_APICallSuccess() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        final String pen = "123456789";
        student.setStudentID(studentID);
        student.setPen(pen);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(List.class)).thenReturn(Mono.just(Arrays.asList(student)));

        val result = this.restUtils.getStudentsByPen(pen, "abc");
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
        assertThat(result.get(0).getPen()).isEqualTo(pen);

        val result2 = this.restUtils.getStudentIDByPen(pen, "abc");
        assertThat(result2).isNotNull();

    }

    @Test
    public void testSaveGraduationStatus_givenValues_returnsGraduationStatus_with_APICallSuccess() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentApiGradStatusUrl(), studentID))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.just(graduationStatus));

        var result = this.restUtils.saveGraduationStudentRecord(graduationStatus, "123");
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
    }

    @Test
    public void testGetStudentsForSpecialGradRun_with_APICallSuccess() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList(pen));

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        GraduationStudentRecordSearchResult res = new GraduationStudentRecordSearchResult();
        res.setStudentIDs(Arrays.asList(graduationStatus.getStudentID()));

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentApiStudentForSpcGradListUrl(), studentID))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordSearchResult.class)).thenReturn(Mono.just(res));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        var result = this.restUtils.getStudentsForSpecialGradRun(req);
        assertThat(result).isNotNull();
        assertThat(result.get(0)).isEqualTo(studentID);
    }

    @Test
    public void testGetStudentsForSpecialGradRun_with_APICallSuccess_null() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList(pen));

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        GraduationStudentRecordSearchResult res = new GraduationStudentRecordSearchResult();
        res.setStudentIDs(Arrays.asList(graduationStatus.getStudentID()));

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentApiStudentForSpcGradListUrl(), studentID))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordSearchResult.class)).thenReturn(inputResponseSR);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.inputResponseSR.block()).thenReturn(null);

        var result = this.restUtils.getStudentsForSpecialGradRun(req);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testProcessStudent() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiUrl(), studentID,batchId))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(alres));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        GraduationStudentRecord response = this.restUtils.processStudent(graduationStatus,summary);
        assertThat(response.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testProcessStudent_witherrors() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiUrl(), studentID,batchId))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(alres));

        GraduationStudentRecord response = this.restUtils.processStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testProcessStudent_witherrors2() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(null);
        alres.setStudentOptionalProgram(null);
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setExceptionName("REPAPI DOWN");
        exceptionMessage.setExceptionDetails("DOWN DOWN DOWN");
        alres.setException(exceptionMessage);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiUrl(), studentID,batchId))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(alres));

        GraduationStudentRecord response = this.restUtils.processStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testProcessProjectedStudent() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(alres));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        GraduationStudentRecord response = this.restUtils.processProjectedGradStudent(graduationStatus,summary);
        assertThat(response.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testProcessProjectedStudent_witherrors() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(alres));

        GraduationStudentRecord response = this.restUtils.processProjectedGradStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testProcessProjectedStudent_witherrors2() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(null);
        alres.setStudentOptionalProgram(null);
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setExceptionName("REPAPI DOWN");
        exceptionMessage.setExceptionDetails("DOWN DOWN DOWN");
        alres.setException(exceptionMessage);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(alres));

        GraduationStudentRecord response = this.restUtils.processProjectedGradStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testGetStudentData_withlist() {

        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        List<UUID> studentList = Arrays.asList(studentID);

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        mockTokenResponseObject();

        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getGradStudentApiStudentDataListUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(graduationStatus)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        List<GraduationStudentRecord> resList =  this.restUtils.getStudentData(studentList);
        assertNotNull(resList);
        assertThat(resList).hasSize(1);
    }

    @Test
    public void testProcessDistribution() {

        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;
        final String mincode = "123121111";
        List<StudentCredentialDistribution> globalList = new ArrayList<>();

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setStudentGrade("12");
        scd.setId(UUID.randomUUID());
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord(mincode);
        scd.setStudentID(studentID);
        globalList.add(scd);




        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd,summary, false);
        assertNotNull(res);
    }

    @Test
    public void testProcessDistribution_elsecase() {

        final UUID studentID = UUID.randomUUID();
        final UUID studentID2 = UUID.randomUUID();
        final Long batchId = 9879L;
        final String mincode = "123121111";
        List<StudentCredentialDistribution> globalList = new ArrayList<>();

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setStudentGrade("12");
        scd.setId(UUID.randomUUID());
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord(mincode);
        scd.setStudentID(studentID);
        globalList.add(scd);

        StudentCredentialDistribution scd2 = new StudentCredentialDistribution();
        scd2.setStudentGrade("12");
        scd2.setId(UUID.randomUUID());
        scd2.setPaperType("YED4");
        scd2.setSchoolOfRecord(mincode);
        scd2.setStudentID(studentID2);


        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID2);
        grd.setProgram("2018-EN");
        grd.setStudentGrade("12");
        grd.setSchoolOfRecord("454445444");

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordDistribution.class)).thenReturn(Mono.just(grd));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd2,summary, false);
        assertNotNull(res);
    }

    @Test
    public void testProcessDistribution_elsecase_null() {

        final UUID studentID = UUID.randomUUID();
        final UUID studentID2 = UUID.randomUUID();
        final Long batchId = 9879L;
        final String mincode = "123121111";
        List<StudentCredentialDistribution> globalList = new ArrayList<>();

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setStudentGrade("12");
        scd.setId(UUID.randomUUID());
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord(mincode);
        scd.setStudentID(studentID);
        globalList.add(scd);

        StudentCredentialDistribution scd2 = new StudentCredentialDistribution();
        scd2.setStudentGrade("12");
        scd2.setId(UUID.randomUUID());
        scd2.setPaperType("YED4");
        scd2.setSchoolOfRecord(mincode);
        scd2.setStudentID(studentID2);

        mockTokenResponseObject();

        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID2);
        grd.setProgram("2018-EN");
        grd.setStudentGrade("12");
        grd.setSchoolOfRecord("454445444");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordDistribution.class)).thenReturn(inputResponseGSR);
        when(this.inputResponseGSR.retryWhen(any())).thenReturn(inputResponseGSR);
        when(this.inputResponseGSR.block()).thenReturn(null);

        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd2,summary, false);
        assertNotNull(res);
    }

    @Test
    public void testProcessBlankDistribution() {
        final String credentialType = "OT";
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType("OT");
        summary.setBatchId(4564L);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setSchoolOfRecord("11231111");
        bcd.setCredentialTypeCode("BC1996-PUB");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }
    @Test
    public void testProcessBlankDistribution_null() {
        final String credentialType = null;
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType(null);
        summary.setBatchId(4564L);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setSchoolOfRecord("11231111");
        bcd.setCredentialTypeCode("BC1996-PUB");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }


    @Test
    public void testProcessBlankDistribution_OC() {
        final String credentialType = "OC";
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType("OC");
        summary.setBatchId(4564L);

        GradCertificateTypes certificateTypes = new GradCertificateTypes();
        certificateTypes.setPaperType("YED2");
        certificateTypes.setCode("E");
        certificateTypes.setDescription("SDS");
        certificateTypes.setLabel("fere");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getCertificateTypes(),"E"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GradCertificateTypes.class)).thenReturn(Mono.just(certificateTypes));

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setSchoolOfRecord("11231111");
        bcd.setCredentialTypeCode("E");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessBlankDistribution_OC_null() {
        final String credentialType = "OC";
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType("OC");
        summary.setBatchId(4564L);

        GradCertificateTypes certificateTypes = new GradCertificateTypes();
        certificateTypes.setPaperType("YED2");
        certificateTypes.setCode("E");
        certificateTypes.setDescription("SDS");
        certificateTypes.setLabel("fere");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getCertificateTypes(),"E"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GradCertificateTypes.class)).thenReturn(inputResponse);
        when(this.inputResponse.block()).thenReturn(null);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setSchoolOfRecord("11231111");
        bcd.setCredentialTypeCode("E");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution() {
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen("1234567");
        scd.setPsiYear("2021");
        scd.setPsiCode("001");
        scd.setStudentID(UUID.randomUUID());
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen("2345678");
        bcd.setPsiCode("002");
        bcd.setPsiYear("2021");
        bcd.setStudentID(UUID.randomUUID());

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution_else() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final String pen2 = "12321312";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen2);
        bcd.setPsiCode("001");
        bcd.setPsiYear("2021");

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen2);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(List.class)).thenReturn(Mono.just(Arrays.asList(student)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution_else_2() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final String pen2 = "12321312";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen2);
        bcd.setPsiCode("001");
        bcd.setPsiYear("2021");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(List.class)).thenReturn(Mono.just(new ArrayList<>()));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution_Exception() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final String pen2 = "12321312";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen2);
        bcd.setPsiCode("001");
        bcd.setPsiYear("2021");
        bcd.setStudentID(studentID);

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen2);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), "1234567"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(new ArrayList<>()));

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
        assertThat(summary.getErrors()).isNotEmpty();
    }

    @Test
    public void testCreateBlankCredentialsAndUpload() {
        final Long batchId = 9879L;

        DistributionResponse res = new DistributionResponse();
        res.setMergeProcessResponse("Done");

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateBlanksAndUpload(),batchId,"N"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(Mono.just(res));

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();
        this.restUtils.createBlankCredentialsAndUpload(batchId,"abc",distributionRequest,"N");
        assertNotNull(res);
    }

    @Test
    public void testCreateBlankCredentialsAndUpload_null() {
        final Long batchId = 9879L;

        DistributionResponse res = new DistributionResponse();
        res.setMergeProcessResponse("Done");

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateBlanksAndUpload(),batchId,"N"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(inputResponsePSI);
        when(this.inputResponsePSI.block()).thenReturn(null);

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();
        this.restUtils.createBlankCredentialsAndUpload(batchId,"abc",distributionRequest,"N");
        assertNotNull(res);
    }

    @Test
    public void testcreateAndStoreSchoolReports_null() {
        final String type = "NONGRADPRJ";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateAndStoreSchoolReports(),type))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(inputResponseI);
        when(this.inputResponseI.block()).thenReturn(null);

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(new ArrayList<>(),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void testcreateAndStoreSchoolReports() {
        final String type = "NONGRADPRJ";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateAndStoreSchoolReports(),type))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(new ArrayList<>(),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void testProcessStudentReports() {
        final String studentReportType = "TVRRUN";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateStudentReport(), studentReportType))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        mockTokenResponseObject();

        var result = this.restUtils.processStudentReports(new ArrayList<>(),studentReportType);
        assertNotNull(studentReportType);
        assertNotNull(result);
    }

    @Test
    public void testcreateAndStoreSchoolReports_0() {
        final String type = "NONGRADPRJ";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateAndStoreSchoolReports(),type))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(0));

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(new ArrayList<>(),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void testRunGradAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        String programCompletionDate = "2020/01";

        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        AlgorithmResponse res = new AlgorithmResponse();
        res.setGraduationStudentRecord(grd);
        res.setStudentOptionalProgram(new ArrayList<>());

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiReportOnlyUrl(), studentID,null))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(res));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.runGradAlgorithm(UUID.fromString(studentID), grd.getProgram(), "123",programCompletionDate,null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testRunGradAlgorithm_programCompletionDateNull() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        String programCompletionDate = "2020/01";

        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        AlgorithmResponse res = new AlgorithmResponse();
        res.setGraduationStudentRecord(grd);
        res.setStudentOptionalProgram(new ArrayList<>());

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiUrl(), studentID,null))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(res));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.runGradAlgorithm(UUID.fromString(studentID), grd.getProgram(), "123",null,null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testRunProjectedGradAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");
        AlgorithmResponse res = new AlgorithmResponse();
        res.setGraduationStudentRecord(grd);
        res.setStudentOptionalProgram(new ArrayList<>());
        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,null))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(AlgorithmResponse.class)).thenReturn(Mono.just(res));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.runProjectedGradAlgorithm(UUID.fromString(studentID), "123",null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetStudentsForAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getGradStudentApiStudentForGradListUrl())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(grd.getStudentID())));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.getStudentsForAlgorithm();
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
    }

    @Test
    public void testGetStudentsForProjectedAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getGradStudentApiStudentForProjectedGradListUrl())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(grd.getStudentID())));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.getStudentsForProjectedAlgorithm();
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
    }

    @Test
    public void testGetStudentForBatchInput() {
        final String mincode = "123213123";
        final UUID studentID = UUID.randomUUID();
        BatchGraduationStudentRecord grd = new BatchGraduationStudentRecord(studentID, "2018-EN", null, "1234567");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGradStudentApiGradStatusForBatchUrl(), studentID))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(BatchGraduationStudentRecord.class)).thenReturn(Mono.just(grd));

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        val result = this.restUtils.getStudentForBatchInput(studentID, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testGetStudentForBatchInput_When_APIisDown_returns_null() {
        final UUID studentID = UUID.randomUUID();
        BatchGraduationStudentRecord grd = new BatchGraduationStudentRecord(studentID, "2018-EN", null, "1234567");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getGradStudentApiGradStatusForBatchUrl(), studentID))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(BatchGraduationStudentRecord.class)).thenReturn(Mono.just(grd));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.runGetStudentForBatchInput(studentID, summary.getAccessToken())).thenThrow(new RuntimeException("GRAD-STUDENT-API is down."));

        val result = this.restUtils.getStudentForBatchInput(studentID, summary);
        assertThat(result).isNull();
    }

    @Test
    public void testGetStudentDataForBatch() {
        final UUID studentID = UUID.randomUUID();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(studentID);
        grd.setProgram("2018-EN");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.just(grd));

        GraduationStudentRecord res = this.restUtils.getStudentDataForBatch(studentID.toString(),null);
        assertThat(res).isNotNull();
        assertThat(res.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testUpdateStudentCredentialRecord() {
        final String studentID = UUID.randomUUID().toString();
        String credentialTypeCode = "E";
        String paperType="YED2";
        String activityCode="USERDISTOC";
        String documentStatusCode="COMPL";
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getUpdateStudentCredential(),studentID,credentialTypeCode,paperType,documentStatusCode,activityCode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(boolean.class)).thenReturn(Mono.just(true));

        this.restUtils.updateStudentCredentialRecord(UUID.fromString(studentID),credentialTypeCode,paperType,documentStatusCode,activityCode,"accessToken");
        assertThat(grd).isNotNull();
    }



    @Test
    public void testGetStudentsForUserReqDisRun() {
        String credentialType = "OT";
        StudentSearchRequest req = new StudentSearchRequest();
        List<String> sch = Arrays.asList("43224223");
        req.setSchoolOfRecords(sch);
        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setSchoolOfRecord("1212211");
        scd.setPaperType("YED2");
        scd.setCredentialTypeCode("E");
        scd.setId(new UUID(1,1));
        scdList.add(scd);

        mockTokenResponseObject();

        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getStudentDataForUserReqDisRun(),credentialType))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(scdList));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.getStudentsForUserReqDisRun(credentialType,req);
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
    }

    @Test
    public void testGetStudentsForUserReqDisRunWithNullDistributionDate() {
        String activityCode = "USERDISTRC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setSchoolOfRecord("1212211");
        scd.setPaperType("YED2");
        scd.setCredentialTypeCode("E");
        scd.setId(new UUID(1,1));
        scdList.add(scd);

        mockTokenResponseObject();

        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getStudentDataForUserReqDisRunWithNullDistributionDate(),activityCode))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(scdList));

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setActivityCode(activityCode);

        val result = this.restUtils.getStudentsForUserReqDisRunWithNullDistributionDate(activityCode,searchRequest);
        assertThat(result).isNotNull();
    }

    @Test
    public void testCreateReprintAndUpload() {
        String activityCode = "USERDISTRC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;
        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getReprintAndUpload(),batchId,activityCode,null))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(Mono.just(req));

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();
        val result = this.restUtils.createReprintAndUpload(batchId,null, distributionRequest, activityCode,null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testCreateReprintAndUpload_null() {
        String activityCode = "USERDISTRC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;
        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getReprintAndUpload(),batchId,activityCode,null))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(inputResponsePSI);
        when(this.inputResponsePSI.block()).thenReturn(null);

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();
        val result = this.restUtils.createReprintAndUpload(batchId,null, distributionRequest, activityCode,null);
        assertThat(result).isNull();
    }

    @Test
    public void testMergeAndUpload() {
        String activityCode = "USERDISTOC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergeAndUpload(),batchId,activityCode,"Y"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(Mono.just(req));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getSchoolDistrictMonthReport())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(4));

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getSchoolDistrictYearEndReport())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(4));

        mockTokenResponseObject();

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();
        val result = this.restUtils.mergeAndUpload(batchId, distributionRequest,activityCode,"Y");
        assertThat(result).isNotNull();
    }

    @Test
    public void testMergeAndUpload_null() {
        String activityCode = "USERDISTOC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergeAndUpload(),batchId,activityCode,"Y"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.retrieve()).thenReturn(this.responseMock);
        when(this.requestBodyMock.body(any())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(inputResponsePSI);
        when(this.retryBackoffSpecMock.filter(any())).thenReturn(retryBackoffSpecMock);
        when(this.retryBackoffSpecMock.onRetryExhaustedThrow(any())).thenReturn(retryBackoffSpecMock);
        when(this.inputResponsePSI.retryWhen(any(reactor.util.retry.Retry.class))).thenReturn(inputResponsePSI);
        when(this.inputResponsePSI.block()).thenReturn(null);

        mockTokenResponseObject();

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();
        val result = this.restUtils.mergeAndUpload(batchId, distributionRequest,activityCode,"Y");
        assertThat(result).isNull();
    }


    @Test
    public void testMergePSIAndUpload() {
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;
        //Grad2-1931
        String transmissionType = "ftp";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergePsiAndUpload(),batchId,"Y"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(Mono.just(req));

        mockTokenResponseObject();

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();
        val result = this.restUtils.mergePsiAndUpload(batchId,null, distributionRequest,"Y", transmissionType);
        assertThat(result).isNotNull();
    }

    @Test
    public void testMergePSIAndUpload_null() {
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;
        String transmissionType = "ftp";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergePsiAndUpload(),batchId,"Y"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(inputResponsePSI);
        when(this.inputResponsePSI.block()).thenReturn(null);

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();
        val result = this.restUtils.mergePsiAndUpload(batchId,null, distributionRequest,"Y",transmissionType);
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetStudentData() {
        final UUID studentID = UUID.randomUUID();
        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID);
        grd.setProgram("2018-EN");

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordDistribution.class)).thenReturn(Mono.just(grd));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        GraduationStudentRecordDistribution res = this.restUtils.getStudentData(studentID.toString());
        assertThat(res).isNotNull();
    }

    @Test
    public void testGetDistrictBySchoolCategoryCode() {
        District district = new District();
        district.setDistrictNumber("042");

        final ParameterizedTypeReference<List<District>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getTraxDistrictBySchoolCategory(), "02"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(List.of(district)));

        List<District> res = this.restUtils.getDistrictBySchoolCategoryCode("02");
        assertThat(res).isNotNull();
    }

    @Test
    public void testGetSchoolBySchoolCategoryCode() {
        School school = new School();
        school.setMincode("1234567");

        final ParameterizedTypeReference<List<School>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getTraxSchoolBySchoolCategory(), "02"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(List.of(school)));

        List<School> res = this.restUtils.getSchoolBySchoolCategoryCode("02");
        assertThat(res).isNotNull();
    }

    @Test
    public void testGetSchoolByDistrictCode() {
        School school = new School();
        school.setMincode("1234567");

        final ParameterizedTypeReference<List<School>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getTraxSchoolByDistrict(), "005"))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(List.of(school)));

        List<School> res = this.restUtils.getSchoolByDistrictCode("005");
        assertThat(res).isNotNull();
    }

    @Test
    public void testExecutePostDistribution() {
        DistributionResponse distributionResponse = new DistributionResponse();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getPostingDistribution())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(Boolean.TRUE));

        Boolean res = this.restUtils.executePostDistribution(distributionResponse);
        assertThat(res).isTrue();
    }

    @Test
    public void testupdateStudentGradRecord() {
        final UUID studentID = UUID.randomUUID();
        final String activityCode = "USERDISOC";
        final Long batchId = 4567L;

        mockTokenResponseObject();

        GraduationStudentRecord rec = new GraduationStudentRecord();
        rec.setStudentID(studentID);
        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateStudentRecord(),studentID,batchId,activityCode))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.retrieve()).thenReturn(this.responseMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.just(rec));

        this.restUtils.updateStudentGradRecord(studentID,batchId,activityCode);
        assertNotNull(rec);

    }

    @Test
    public void testUpdateStudentGradRecordHistory() {
        final UUID studentID = UUID.randomUUID();
        final String userName = "abc";
        final String accessToken = "xyz";
        final Long batchId = 4567L;

        GraduationStudentRecord rec = new GraduationStudentRecord();
        rec.setStudentID(studentID);
        when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateStudentRecordHistory(),studentID, batchId, accessToken, userName))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.retrieve()).thenReturn(this.responseMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.just(rec));

        this.restUtils.updateStudentGradRecordHistory(List.of(), batchId, accessToken, userName, null);

        when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateStudentRecordHistory(), batchId, userName, "USERSTUDARC"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.retrieve()).thenReturn(this.responseMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.empty());

        mockTokenResponseObject();

        this.restUtils.updateStudentGradRecordHistory(List.of(studentID), batchId, userName, "USERSTUDARC");
        assertNotNull(rec);

    }

    @Test
    public void testUpdateSchoolReportRecord() {
        final String mincode = "123213123";
        String reportTypeCode = "E";

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getUpdateSchoolReport(),mincode,reportTypeCode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(boolean.class)).thenReturn(Mono.just(true));

        mockTokenResponseObject();

        restUtils.updateSchoolReportRecord(mincode,reportTypeCode,null);
        assertThat(reportTypeCode).isEqualTo("E");

        when(this.webClient.delete()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getUpdateSchoolReport(),mincode,reportTypeCode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(boolean.class)).thenReturn(Mono.just(true));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        restUtils.deleteSchoolReportRecord(mincode,reportTypeCode);
        assertThat(reportTypeCode).isEqualTo("E");
    }

    @Test
    public void testDeleteSchoolReportRecord() {
        final String mincode = "123213123";
        String reportTypeCode = "E";

        mockTokenResponseObject();

        when(this.webClient.delete()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getUpdateSchoolReport(),mincode,reportTypeCode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(boolean.class)).thenReturn(Mono.just(true));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        this.restUtils.deleteSchoolReportRecord(mincode,reportTypeCode);
        assertThat(reportTypeCode).isEqualTo("E");
    }

    @Test
    public void testGetStudentByPenFromStudentAPI() {
        final UUID studentID = UUID.randomUUID();

        final String pen = "123456789";

        List<LoadStudentData> loadStudentData = new ArrayList<>();
        LoadStudentData lSD = new LoadStudentData();
        lSD.setPen(pen);
        lSD.setStudentGrade("12");
        lSD.setHonoursStanding("N");
        lSD.setProgramCode("2018-EN");
        loadStudentData.add(lSD);

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(List.class)).thenReturn(Mono.just(Arrays.asList(student)));

        mockTokenResponseObject();

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentApiGradStatusUrl(), studentID))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.just(graduationStatus));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        Integer res = this.restUtils.getStudentByPenFromStudentAPI(loadStudentData, "accessToken");
        assertThat(res).isEqualTo(1);

    }

    @Test
    public void testUpdateStudentFlagReadyForBatch() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final String batchJobType = "REGALG";

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        List<UUID> studentIDs = Arrays.asList(studentID);

        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIDs);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateStudentFlagReadyForBatchByStudentIDs(), batchJobType))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        final ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just("SUCCESS"));

        var result = this.restUtils.updateStudentFlagReadyForBatch(studentIDs, batchJobType, "abc");
        assertThat(stuList).isNotNull();
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    public void testIsReportOnly_when_programCompletionDate_isInFuture_thenReturns_GS() {
        final UUID studentID = UUID.randomUUID();
        final String gradProgram = "SCCP";

        Date futureDate = DateUtils.addMonths(new Date(), 1);
        final String programCompletionDate = EducGradBatchGraduationApiUtils.formatDate(futureDate, "yyyy/MM");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(constants.getCheckSccpCertificateExists()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));

        val result = this.restUtils.isReportOnly(studentID, gradProgram, programCompletionDate, "abc");
        assertThat(result).isFalse();
    }

    @Test
    public void testIsReportOnly_when_programCompletionDate_isNotInFuture_and_SCCPcertificateExists_thenReturns_FMR() {
        final UUID studentID = UUID.randomUUID();
        final String gradProgram = "SCCP";
        final String programCompletionDate = "2023/01";

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(constants.getCheckSccpCertificateExists()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));

        val result = this.restUtils.isReportOnly(studentID, gradProgram, programCompletionDate, "abc");
        assertThat(result).isTrue();
    }

    @Test
    public void testRunRegenerateStudentCertificates() {
        final String pen = "123456789";

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(String.format(constants.getStudentCertificateRegeneration(), pen)), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(Integer.valueOf(1)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.runRegenerateStudentCertificate(pen);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testFetchDistributionRequiredDataStudentsNonGradYearly() {
        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentDataNonGradEarly()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){})).thenReturn(Mono.just(List.of(reportGradStudentData)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.fetchDistributionRequiredDataStudentsNonGradYearly();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testFetchDistributionRequiredDataStudentsNonGradYearlyByMincode() {
        String mincode = "1234567";
        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentDataNonGradEarlyByMincode(), mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){})).thenReturn(Mono.just(List.of(reportGradStudentData)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.fetchDistributionRequiredDataStudentsNonGradYearly(mincode);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testFetchDistributionRequiredDataStudentsYearly() {
        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentReportDataYearly()))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){})).thenReturn(Mono.just(List.of(reportGradStudentData)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.fetchDistributionRequiredDataStudentsYearly();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testGetEDWSnapshotSchools() {
        final Integer gradYear = Integer.parseInt("2023");

        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getEdwSnapshotSchoolsUrl(), gradYear))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<String>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(schools));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.getEDWSnapshotSchools(gradYear);
        assertThat(result).hasSize(2);
    }

    @Test
    public void testGetTotalSchoolReportsForArchiving() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradSchoolReportsCountUrl(), "GRADREG"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getTotalReportsForProcessing(schools, "GRADREG", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGetReportStudentIDsByStudentIDsAndReportType() {
        UUID uuid = UUID.randomUUID();
        List<String> studentIDsIn = Arrays.asList(uuid.toString());
        List<UUID> studentIDsOut = Arrays.asList(uuid);

        mockTokenResponseObject();

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentReportsGuidsUrl(), "ACHV", 1))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(studentIDsOut));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getReportStudentIDsByStudentIDsAndReportType(studentIDsIn, "ACHV", 1, summaryDTO);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testGetReportStudentIDsByStudentIDsAndReportTypeError() {
        UUID uuid = UUID.randomUUID();
        List<String> studentIDsIn = Arrays.asList(uuid.toString());
        List<UUID> studentIDsOut = Arrays.asList(uuid);

        mockTokenResponseObject();

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentReportsGuidsUrl(), "ACHV", 1))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to retrieve report student guids"));
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(studentIDsOut));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getReportStudentIDsByStudentIDsAndReportType(studentIDsIn, "ACHV", 1, summaryDTO);
        assertThat(result).isEmpty();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetStudentIDsBySearchCriteriaOrAll() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setStudentIDs(studentIDs);
        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getGradGetStudentsBySearchCriteriaUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(studentIDs));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getStudentIDsBySearchCriteriaOrAll(searchRequest, summaryDTO);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testGetStudentIDsBySearchCriteriaOrAllError() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setStudentIDs(studentIDs);
        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getGradGetStudentsBySearchCriteriaUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to retrieve list of Students"));
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(studentIDs));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getStudentIDsBySearchCriteriaOrAll(searchRequest, summaryDTO);
        assertThat(result).isEmpty();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetTotalSchoolReportsForArchivingError() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradSchoolReportsCountUrl(), "GRADREG"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to retrieve school reports counts"));
        when(this.responseMock.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getTotalReportsForProcessing(schools, "GRADREG", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testDeleteStudentReports() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getDeleteStudentReportsUrl(), 12345678L, "ACHV"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.deleteStudentReports(12345678L, studentIDs,"ACHV", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testDeleteStudentReportsError() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getDeleteStudentReportsUrl(), 12345678L, "ACHV"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to delete student reports"));
        when(this.responseMock.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.deleteStudentReports(12345678L, studentIDs,"ACHV", summaryDTO);
        assertThat(result).isZero();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testArchiveSchoolReports() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradArchiveSchoolReportsUrl(), 12345678L, "GRADREG"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.archiveSchoolReports(12345678L, schools,"GRADREG", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testArchiveSchoolReportsError() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradArchiveSchoolReportsUrl(), 12345678L, "GRADREG"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to archive School Reports"));
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(0));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.archiveSchoolReports(12345678L, schools,"GRADREG", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetTotalStudentsForArchiving() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentCountUrl(), "CUR"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(schools, "CUR", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGetTotalStudentsForArchivingError() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentCountUrl(), "CUR"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to retrieve student counts"));
        when(this.responseMock.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(schools, "CUR", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testArchiveStudents() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradArchiveStudentsUrl(), 12345678L, "CUR", "USER"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setUserName("USER");

        val result = this.restUtils.archiveStudents(12345678L, schools,"CUR", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testArchiveStudentsError() {
        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradArchiveStudentsUrl(), 12345678L, "CUR", "USER"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenThrow(new RuntimeException("Unable to archive Students"));
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(0));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setUserName("USER");

        val result = this.restUtils.archiveStudents(12345678L, schools,"CUR", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetEDWSnapshotStudents() {
        final Integer gradYear = Integer.parseInt("2023");
        final String mincode = "12345678";

        SnapshotResponse snapshotResponse = new SnapshotResponse();
        snapshotResponse.setPen("123456789");
        snapshotResponse.setSchoolOfRecord(mincode);
        snapshotResponse.setGraduatedDate("202306");
        snapshotResponse.setGpa(BigDecimal.valueOf(3.75));
        snapshotResponse.setHonourFlag("N");

        mockTokenResponseObject();

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getEdwSnapshotStudentsByMincodeUrl(), gradYear, mincode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<SnapshotResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(snapshotResponse)));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.getEDWSnapshotStudents(gradYear, mincode);
        assertThat(result).hasSize(1);
    }

    @Test
    public void testProcessSnapshot() {
        final Integer gradYear = Integer.parseInt("2023");
        final String mincode = "12345678";

        EdwGraduationSnapshot snapshot = new EdwGraduationSnapshot();
        snapshot.setStudentID(UUID.randomUUID());
        snapshot.setPen("123456789");
        snapshot.setGradYear(gradYear);
        snapshot.setSchoolOfRecord(mincode);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getSnapshotGraduationStatusForEdwUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(EdwGraduationSnapshot.class)).thenReturn(Mono.just(snapshot));

        val result = this.restUtils.processSnapshot(snapshot, new EdwSnapshotSummaryDTO());
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(snapshot.getPen());
    }

    @Test
    public void testProcessSnapshotException() {
        final Integer gradYear = Integer.parseInt("2023");
        final String mincode = "12345678";

        EdwGraduationSnapshot snapshot = new EdwGraduationSnapshot();
        snapshot.setStudentID(UUID.randomUUID());
        snapshot.setPen("123456789");
        snapshot.setGradYear(gradYear);
        snapshot.setSchoolOfRecord(mincode);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getSnapshotGraduationStatusForEdwUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenThrow(new RuntimeException());
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(EdwGraduationSnapshot.class)).thenReturn(Mono.just(snapshot));

        EdwSnapshotSummaryDTO summaryDTO = new EdwSnapshotSummaryDTO();

        val result = this.restUtils.processSnapshot(snapshot, summaryDTO);
        assertThat(result).isNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }


    @Test
    public void testGetDeceasedStudentIDs() {
        final UUID studentID1 = UUID.randomUUID();
        final UUID studentID2 = UUID.randomUUID();

        List<UUID> studentIDs = Arrays.asList(studentID1, studentID2);

        mockTokenResponseObject();

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getDeceasedStudentIDList())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(studentIDs));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        val result = this.restUtils.getDeceasedStudentIDs(studentIDs);
        assertThat(result).hasSize(2);
    }

    @Test
    public void testFetchAccessToken() {
        mockTokenResponseObject();
        String result = this.restUtils.fetchAccessToken();
        assertThat(result).isNotNull();
    }

    private String mockTokenResponseObject() {
        final ResponseObj tokenObject = new ResponseObj();
        String mockToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJtbUhsTG4tUFlpdTl3MlVhRnh5Yk5nekQ3d2ZIb3ZBRFhHSzNROTk0cHZrIn0.eyJleHAiOjE2NjMxODg1MzMsImlhdCI6MTY2MzE4ODIzMywianRpIjoiZjA2ZWJmZDUtMzRlMi00NjY5LTg0MDktOThkNTc3OGZiYmM3IiwiaXNzIjoiaHR0cHM6Ly9zb2FtLWRldi5hcHBzLnNpbHZlci5kZXZvcHMuZ292LmJjLmNhL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI4ZGFjNmM3Yy0xYjU5LTQ5ZDEtOTMwNC0wZGRkMTdlZGE0YWQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJncmFkLWFkbWluLWNsaWVudCIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly9kZXYuZ3JhZC5nb3YuYmMuY2EiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6IldSSVRFX1NUVURFTlQgR1JBRF9CVVNJTkVTU19SIENSRUFURV9TVFVERU5UX1hNTF9UUkFOU0NSSVBUX1JFUE9SVCBDUkVBVEVfR1JBRF9BU1NFU1NNRU5UX1JFUVVJUkVNRU5UX0RBVEEgUkVBRF9TVFVERU5UIFJFQURfU0NIT09MIGVtYWlsIHByb2ZpbGUiLCJjbGllbnRJZCI6ImdyYWQtYWRtaW4tY2xpZW50IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTQyLjMxLjQwLjE1NiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1ncmFkLWFkbWluLWNsaWVudCIsImNsaWVudEFkZHJlc3MiOiIxNDIuMzEuNDAuMTU2In0.AqSxYzfanjhxCEuxLVHcJWA528AglXezS0-6EBohLsAJ4W1prdcrcS7p6yv1mSBs9GEkCu7SZhjl97xWaNXf7Emd4O0ieawgfXhDdgCtWtpLc0X2NjRTcZmv9kCpr__LmX4Zl3temUShNLVsSI95iBD7GKQmx_qTMpf3fiXdmmBvpZIibEly9RBbrio5DirqdYKuj0CO3x7xruBdBQnutr_GK7_vkmpw-X4RAyxsCwxSDequot1cCgMcJvPb6SxOL0BHx01OjM84FPwf2DwDrLvhXXhh4KucykUJ7QfiA5unmlLQ0wfG-bBJDwpjlXazF8jOQNEcasABVTftW6s8NA";
        tokenObject.setAccess_token(mockToken);
        tokenObject.setRefresh_token("456");

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getTokenUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(ResponseObj.class)).thenReturn(Mono.just(tokenObject));
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);

        return mockToken;
    }
}
