package ca.bc.gov.educ.api.batchgraduation.util;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.*;
import java.util.function.Consumer;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import lombok.val;
import reactor.core.publisher.Mono;

import javax.validation.constraints.AssertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RestUtilsTest {
    @Autowired
    RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Autowired
    private EducGradBatchGraduationApiConstants constants;

    @Mock
    private Mono<GradCertificateTypes> inputResponse;
    @Mock
    private Mono<GraduationStudentRecordDistribution> inputResponseGSR;
    @Mock
    private Mono<GraduationStudentRecordSearchResult> inputResponseSR;

    @Mock
    private Mono<DistributionResponse> inputResponsePSI;

    @Mock
    private Mono<Integer> inputResponseI;

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

        val result = this.restUtils.getTokenResponseObject();
        assertThat(result).isNotNull();
        assertThat(result.getAccess_token()).isEqualTo(mockToken);
        assertThat(result.getRefresh_token()).isEqualTo("456");
    }

    public void testGetTokenResponseObject_givenExpiredToken_shouldReturnNewToken() {

    }

    @Test
    public void testFallBackMethod_givenExcpetion_shouldReturnNull(){
        val result = this.restUtils.rtGetTokenFallBack(new HttpServerErrorException(HttpStatus.I_AM_A_TEAPOT));
        assertThat(result).isNull();
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

        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(student)));

        val result = this.restUtils.getStudentsByPen(pen, "abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
        assertThat(result.get(0).getPen()).isEqualTo(pen);
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
        res.setGraduationStudentRecords(Arrays.asList(graduationStatus));


        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentApiStudentForSpcGradListUrl(), studentID))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordSearchResult.class)).thenReturn(Mono.just(res));


        var result = this.restUtils.getStudentsForSpecialGradRun(req, "123");
        assertThat(result).isNotNull();
        assertThat(result.get(0).getPen()).isEqualTo(pen);
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
        res.setGraduationStudentRecords(Arrays.asList(graduationStatus));


        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getGradStudentApiStudentForSpcGradListUrl(), studentID))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordSearchResult.class)).thenReturn(inputResponseSR);
        when(this.inputResponseSR.block()).thenReturn(null);

        var result = this.restUtils.getStudentsForSpecialGradRun(req, "123");
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

        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(constants.getGradStudentApiStudentDataListUrl())).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(graduationStatus)));

        List<GraduationStudentRecord> resList =  this.restUtils.getStudentData(studentList,"abc");
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

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd,summary);
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

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordDistribution.class)).thenReturn(Mono.just(grd));

        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd2,summary);
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


        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID2);
        grd.setProgram("2018-EN");
        grd.setStudentGrade("12");
        grd.setSchoolOfRecord("454445444");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordDistribution.class)).thenReturn(inputResponseGSR);
        when(this.inputResponseGSR.block()).thenReturn(null);

        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd2,summary);
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
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setPsiCode("001");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen);
        bcd.setPsiCode("002");
        bcd.setPsiYear("2021");

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

        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(student)));

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

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen2);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPenStudentApiByPenUrl(), pen2))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(new ArrayList<>()));

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
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

        this.restUtils.createBlankCredentialsAndUpload(batchId,"abc",new HashMap<>(),"N");
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

        this.restUtils.createBlankCredentialsAndUpload(batchId,"abc",new HashMap<>(),"N");
        assertNotNull(res);
    }



    @Test
    public void testcreateAndStoreSchoolReports_null() {
        final String type = "NONGRADPRJ";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateAndStore(),type))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(inputResponseI);
        when(this.inputResponseI.block()).thenReturn(null);

        this.restUtils.createAndStoreSchoolReports("Abc",new ArrayList<>(),type);
        assertNotNull(type);
    }

    @Test
    public void testcreateAndStoreSchoolReports() {
        final String type = "NONGRADPRJ";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateAndStore(),type))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(1));

        this.restUtils.createAndStoreSchoolReports("Abc",new ArrayList<>(),type);
        assertNotNull(type);
    }

    @Test
    public void testcreateAndStoreSchoolReports_0() {
        final String type = "NONGRADPRJ";

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getCreateAndStore(),type))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(Integer.class)).thenReturn(Mono.just(0));

        this.restUtils.createAndStoreSchoolReports("Abc",new ArrayList<>(),type);
        assertNotNull(type);
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

        val result = this.restUtils.runGradAlgorithm(UUID.fromString(studentID), "123",programCompletionDate,null);
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

        val result = this.restUtils.runGradAlgorithm(UUID.fromString(studentID), "123",null,null);
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

        val result = this.restUtils.runProjectedGradAlgorithm(UUID.fromString(studentID), "123",null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetStudentsForAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getGradStudentApiStudentForGradListUrl())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(grd.getStudentID())));

        val result = this.restUtils.getStudentsForAlgorithm("abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
    }

    @Test
    public void testGetStudentsForProjectedAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getGradStudentApiStudentForProjectedGradListUrl())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(grd.getStudentID())));

        val result = this.restUtils.getStudentsForProjectedAlgorithm("abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
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

        when(this.responseMock.bodyToMono(BatchGraduationStudentRecord.class)).thenReturn(Mono.just(grd));

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        val result = this.restUtils.getStudentForBatchInput(studentID, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testUpdateStudentCredentialRecord() {
        final String studentID = UUID.randomUUID().toString();
        String credentialTypeCode = "E";
        String paperType="YED2";
        String documentStatusCode="COMPL";
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getUpdateStudentCredential(),studentID,credentialTypeCode,paperType,documentStatusCode))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(boolean.class)).thenReturn(Mono.just(true));

        this.restUtils.updateStudentCredentialRecord(UUID.fromString(studentID),credentialTypeCode,paperType,documentStatusCode,null);
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

        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getStudentDataForUserReqDisRun(),credentialType))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(scdList));


        val result = this.restUtils.getStudentsForUserReqDisRun(credentialType,req,null);
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
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


        val result = this.restUtils.createReprintAndUpload(batchId,null,new HashMap<>(), activityCode,null);
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


        val result = this.restUtils.createReprintAndUpload(batchId,null,new HashMap<>(), activityCode,null);
        assertThat(result).isNull();
    }

    @Test
    public void testMergeAndUpload() {
        String activityCode = "USERDISTOC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergeAndUpload(),batchId,activityCode,null))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(Mono.just(req));


        val result = this.restUtils.mergeAndUpload(batchId,null,new HashMap<>(),activityCode,null);
        assertThat(result).isNotNull();
    }
    @Test
    public void testMergeAndUpload_null() {
        String activityCode = "USERDISTOC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergeAndUpload(),batchId,activityCode,null))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(inputResponsePSI);
        when(this.inputResponsePSI.block()).thenReturn(null);


        val result = this.restUtils.mergeAndUpload(batchId,null,new HashMap<>(),activityCode,null);
        assertThat(result).isNull();
    }


    @Test
    public void testMergePSIAndUpload() {
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergePsiAndUpload(),batchId,"Y"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(Mono.just(req));


        val result = this.restUtils.mergePsiAndUpload(batchId,null,new HashMap<>(),"Y");
        assertThat(result).isNotNull();
    }

    @Test
    public void testMergePSIAndUpload_null() {
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getMergePsiAndUpload(),batchId,"Y"))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(DistributionResponse.class)).thenReturn(inputResponsePSI);
        when(this.inputResponsePSI.block()).thenReturn(null);


        val result = this.restUtils.mergePsiAndUpload(batchId,null,new HashMap<>(),"Y");
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetStudentData() {
        final UUID studentID = UUID.randomUUID();
        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID);
        grd.setProgram("2018-EN");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getStudentInfo(),studentID))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecordDistribution.class)).thenReturn(Mono.just(grd));

        GraduationStudentRecordDistribution res = this.restUtils.getStudentData(studentID.toString(),null);
        assertThat(res).isNotNull();
    }

    @Test
    public void testupdateStudentGradRecord() {
        final UUID studentID = UUID.randomUUID();
        final String activityCode = "USERDISOC";
        final Long batchId = 4567L;

        GraduationStudentRecord rec = new GraduationStudentRecord();
        rec.setStudentID(studentID);
        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(String.format(constants.getUpdateStudentRecord(),studentID,batchId,activityCode))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStudentRecord.class)).thenReturn(Mono.just(rec));

        this.restUtils.updateStudentGradRecord(studentID,batchId,activityCode,"acb");
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

        this.restUtils.updateSchoolReportRecord(mincode,reportTypeCode,null);
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

        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(student)));


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


        Integer res = this.restUtils.getStudentByPenFromStudentAPI(loadStudentData,"abc");
        assertThat(res).isEqualTo(1);

    }

}
