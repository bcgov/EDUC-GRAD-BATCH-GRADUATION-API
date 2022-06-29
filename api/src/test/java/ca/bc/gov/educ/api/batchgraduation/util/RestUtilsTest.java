package ca.bc.gov.educ.api.batchgraduation.util;


import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import lombok.val;
import reactor.core.publisher.Mono;

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
        tokenObject.setAccess_token("123");
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
        assertThat(result.getAccess_token()).isEqualTo("123");
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

        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(grd)));

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

        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(grd)));

        val result = this.restUtils.getStudentsForProjectedAlgorithm("abc");
        assertThat(result).isNotNull();
        assertThat(result.size() > 0).isTrue();
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

}
