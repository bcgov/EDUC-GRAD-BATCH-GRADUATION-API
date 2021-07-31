package ca.bc.gov.educ.api.batchgraduation.util;


import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import lombok.val;
import org.codehaus.jackson.JsonProcessingException;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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
    public void testGetSpecialProgram_givenValues_returnsGradSpecialProgram_with_APICallSuccess() throws JsonProcessingException {
        final UUID specialProgramID = UUID.randomUUID();
        final GradSpecialProgram specialProgram = new GradSpecialProgram();
        specialProgram.setId(specialProgramID);
        specialProgram.setProgramCode("abc");
        specialProgram.setSpecialProgramCode("def");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradProgramApiOptionalProgramUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GradSpecialProgram.class)).thenReturn(Mono.just(specialProgram));
        val result = this.restUtils.getGradSpecialProgram("abc", "def", "123");
        assertThat(result).isNotNull();
        assertThat(result.getProgramCode()).isEqualTo("abc");
        assertThat(result.getSpecialProgramCode()).isEqualTo("def");
    }

    @Test
    public void testGetGraduationStatus_givenValues_returnsGraduationStatus_with_APICallSuccess() throws JsonProcessingException {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        GraduationStatus graduationStatus = new GraduationStatus();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradStudentApiGradStatusUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GraduationStatus.class)).thenReturn(Mono.just(graduationStatus));

        var result = this.restUtils.getGraduationStatus(pen, "123");
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
    }

    @Test
    public void testSaveGraduationStatus_givenValues_returnsGraduationStatus_with_APICallSuccess() throws JsonProcessingException {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        GraduationStatus graduationStatus = new GraduationStatus();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(eq(constants.getGradStudentApiGradStatusUrl()), any(Function.class))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GraduationStatus.class)).thenReturn(Mono.just(graduationStatus));

        var result = this.restUtils.saveGraduationStatus(graduationStatus, "123");
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
    }

    @Test
    public void testGetStudentSpecialProgram_givenValues_returnsGradSpecialProgram_with_APICallSuccess() throws JsonProcessingException {
        final UUID ID = UUID.randomUUID();
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final UUID specialProgramID = UUID.randomUUID();

        GradStudentSpecialProgram gradStudentSpecialProgram = new GradStudentSpecialProgram();
        gradStudentSpecialProgram.setId(ID);
        gradStudentSpecialProgram.setPen(pen);
        gradStudentSpecialProgram.setSpecialProgramID(specialProgramID);
        gradStudentSpecialProgram.setMainProgramCode("2018-EN");
        gradStudentSpecialProgram.setSpecialProgramCode("Special");
        gradStudentSpecialProgram.setSpecialProgramName("Test Special Course");

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getGradProgramApiOptionalProgramUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GradStudentSpecialProgram.class)).thenReturn(Mono.just(gradStudentSpecialProgram));

        var result = this.restUtils.getStudentSpecialProgram(studentID, specialProgramID, "123");
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getSpecialProgramID()).isEqualTo(specialProgramID);
        assertThat(result.getSpecialProgramCode()).isEqualTo(gradStudentSpecialProgram.getSpecialProgramCode());
    }

    @Test
    public void testSaveStudentSpecialProgram_givenValues_returnsGradSpecialProgram_with_APICallSuccess() throws JsonProcessingException {
        final UUID ID = UUID.randomUUID();
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final UUID specialProgramID = UUID.randomUUID();

        GradStudentSpecialProgram gradStudentSpecialProgram = new GradStudentSpecialProgram();
        gradStudentSpecialProgram.setId(ID);
        gradStudentSpecialProgram.setPen(pen);
        gradStudentSpecialProgram.setSpecialProgramID(specialProgramID);
        gradStudentSpecialProgram.setMainProgramCode("2018-EN");
        gradStudentSpecialProgram.setSpecialProgramCode("Special");
        gradStudentSpecialProgram.setSpecialProgramName("Test Special Course");

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(eq(constants.getGradProgramApiOptionalProgramUrl()), any(Function.class))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GradStudentSpecialProgram.class)).thenReturn(Mono.just(gradStudentSpecialProgram));

        var result = this.restUtils.saveStudentSpecialProgram(gradStudentSpecialProgram, "123");
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getSpecialProgramID()).isEqualTo(specialProgramID);
        assertThat(result.getSpecialProgramCode()).isEqualTo(gradStudentSpecialProgram.getSpecialProgramCode());
    }

    @Test
    public void testCheckFrenchImmersionCourse_givenValues_returnFalse() throws JsonProcessingException {
        final String pen = "123456789";

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCheckFrenchImmersionCourseUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));

        var result = this.restUtils.checkFrenchImmersionCourse(pen, "123");
        assertThat(result).isNotNull();
        assertThat(result).isTrue();
    }

    @Test
    public void testGetCourseRestrictions_givenValues_returnsGradCourseRestriction_with_APICallSuccess() throws JsonProcessingException {
        final UUID courseRestrictionID = UUID.randomUUID();

        GradCourseRestriction gradCourseRestriction = new GradCourseRestriction();
        gradCourseRestriction.setMainCourse("Main");
        gradCourseRestriction.setMainCourseLevel("12");
        gradCourseRestriction.setRestrictedCourse("Rest");
        gradCourseRestriction.setRestrictedCourseLevel("12");
        gradCourseRestriction.setCourseRestrictionId(courseRestrictionID);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCourseRestrictionUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        final ParameterizedTypeReference<List<GradCourseRestriction>> responseType = new ParameterizedTypeReference<>() {
        };
        when(this.responseMock.bodyToMono(responseType)).thenReturn(Mono.just(Arrays.asList(gradCourseRestriction)));

        var result = this.restUtils.getCourseRestrictions("Main", "Rest", "123");
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        GradCourseRestriction responseData = result.get(0);
        assertThat(responseData.getCourseRestrictionId()).isEqualTo(courseRestrictionID);
        assertThat(responseData.getMainCourse()).isEqualTo(gradCourseRestriction.getMainCourse());
        assertThat(responseData.getRestrictedCourse()).isEqualTo(gradCourseRestriction.getRestrictedCourse());
    }

    @Test
    public void testGetCourseRestriction_givenValues_returnsGradCourseRestriction_with_APICallSuccess() throws JsonProcessingException {
        final UUID courseRestrictionID = UUID.randomUUID();

        GradCourseRestriction gradCourseRestriction = new GradCourseRestriction();
        gradCourseRestriction.setMainCourse("Main");
        gradCourseRestriction.setMainCourseLevel("12");
        gradCourseRestriction.setRestrictedCourse("Rest");
        gradCourseRestriction.setRestrictedCourseLevel("12");
        gradCourseRestriction.setCourseRestrictionId(courseRestrictionID);

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(eq(this.constants.getCourseRestrictionUrl()), any(Function.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

        when(this.responseMock.bodyToMono(GradCourseRestriction.class)).thenReturn(Mono.just(gradCourseRestriction));

        var result = this.restUtils.getCourseRestriction("Main", "12", "Rest", "12", "123");
        assertThat(result).isNotNull();
        assertThat(result.getCourseRestrictionId()).isEqualTo(courseRestrictionID);
        assertThat(result.getMainCourse()).isEqualTo(gradCourseRestriction.getMainCourse());
        assertThat(result.getRestrictedCourse()).isEqualTo(gradCourseRestriction.getRestrictedCourse());
    }

    @Test
    public void testSaveCourseRestriction_givenValues_returnsGradCourseRestriction_with_APICallSuccess() throws JsonProcessingException {
        final UUID courseRestrictionID = UUID.randomUUID();

        GradCourseRestriction gradCourseRestriction = new GradCourseRestriction();
        gradCourseRestriction.setMainCourse("Main");
        gradCourseRestriction.setMainCourseLevel("12");
        gradCourseRestriction.setRestrictedCourse("Rest");
        gradCourseRestriction.setRestrictedCourseLevel("12");
        gradCourseRestriction.setCourseRestrictionId(courseRestrictionID);

        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(eq(constants.getCourseRestrictionUrl()))).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(GradCourseRestriction.class)).thenReturn(Mono.just(gradCourseRestriction));

        var result = this.restUtils.saveCourseRestriction(gradCourseRestriction, "123");
        assertThat(result.getCourseRestrictionId()).isEqualTo(courseRestrictionID);
        assertThat(result.getMainCourse()).isEqualTo(gradCourseRestriction.getMainCourse());
        assertThat(result.getRestrictedCourse()).isEqualTo(gradCourseRestriction.getRestrictedCourse());
    }
}
