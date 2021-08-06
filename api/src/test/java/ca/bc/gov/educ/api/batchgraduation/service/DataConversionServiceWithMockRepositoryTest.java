package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.TraxStudentRepository;
import ca.bc.gov.educ.api.batchgraduation.util.GradBatchTestUtils;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DataConversionServiceWithMockRepositoryTest {

    @Autowired
    DataConversionService dataConversionService;

    @MockBean
    TraxStudentRepository traxStudentRepository;

    @MockBean
    RestUtils restUtils;

    @Autowired
    GradBatchTestUtils gradBatchTestUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() { }

    @Test
    public void convertStudent_forExistingGradStudent_whenGivenData_withFrechImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStatus graduationStatus = new GraduationStatus();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram gradStudentSpecialProgram = new StudentOptionalProgram();
        gradStudentSpecialProgram.setId(UUID.randomUUID());
        gradStudentSpecialProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        gradStudentSpecialProgram.setPen(pen);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getStudentByPen(eq(pen), eq(summary.getAccessToken()))).thenReturn(penStudent);
        when(this.restUtils.saveGraduationStatus(any(GraduationStatus.class), eq(summary.getAccessToken()))).thenReturn(graduationStatus);
        when(this.restUtils.checkFrenchImmersionCourse(eq(pen), eq(summary.getAccessToken()))).thenReturn(Boolean.TRUE);
        when(this.restUtils.saveStudentSpecialProgram(eq(gradStudentSpecialProgram), eq(summary.getAccessToken()))).thenReturn(gradStudentSpecialProgram);
        when(this.restUtils.getStudentsByPen(eq(pen), eq(summary.getAccessToken()))).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradSpecialProgram(eq("2018-EN"), eq("FI"), eq(summary.getAccessToken()))).thenReturn(specialProgram);

        var result = dataConversionService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void convertStudent_whenGivenData_withFrechImmersionSpecialProgram_thenReturnSuccess() throws Exception {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "111222333";

        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen(pen);

        GraduationStatus graduationStatus = new GraduationStatus();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        OptionalProgram specialProgram = new OptionalProgram();
        specialProgram.setOptionalProgramID(UUID.randomUUID());
        specialProgram.setGraduationProgramCode("2018-EN");
        specialProgram.setOptProgramCode("FI");
        specialProgram.setOptionalProgramName("French Immersion");

        StudentOptionalProgram gradStudentSpecialProgram = new StudentOptionalProgram();
        gradStudentSpecialProgram.setId(UUID.randomUUID());
        gradStudentSpecialProgram.setOptionalProgramID(specialProgram.getOptionalProgramID());
        gradStudentSpecialProgram.setPen(pen);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-EN").recalculateGradStatus("Y")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.getStudentByPen(eq(pen), eq(summary.getAccessToken()))).thenReturn(penStudent);
        when(this.restUtils.saveGraduationStatus(any(GraduationStatus.class), eq(summary.getAccessToken()))).thenReturn(graduationStatus);
        when(this.restUtils.checkFrenchImmersionCourse(eq(pen), eq(summary.getAccessToken()))).thenReturn(Boolean.TRUE);
        when(this.restUtils.saveStudentSpecialProgram(eq(gradStudentSpecialProgram), eq(summary.getAccessToken()))).thenReturn(gradStudentSpecialProgram);
        when(this.restUtils.getStudentsByPen(eq(pen), eq(summary.getAccessToken()))).thenReturn(Arrays.asList(penStudent));
        when(this.restUtils.getGradSpecialProgram(eq("2018-EN"), eq("FI"), eq(summary.getAccessToken()))).thenReturn(specialProgram);

        var result = dataConversionService.convertStudent(student, summary);

        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
        assertThat(result.getRecalculateGradStatus()).isEqualTo("Y");
        assertThat(result.getProgram()).isEqualTo(specialProgram.getGraduationProgramCode());

    }

    @Test
    public void testLoadInitialRawGradStudentData() {
        Object[] obj = new Object[] {
               "123456789", "12345678", "12345678", "12", Character.valueOf('A'), "2020", Character.valueOf('Y')
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentRepository.loadInitialRawData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradStudentData(true);
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        ConvGradStudent responseStudent = result.get(0);
        assertThat(responseStudent.getPen()).isEqualTo(obj[0]);
    }

    @Test
    public void testConvertCourseRestriction() {
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        CourseRestriction courseRestriction = new CourseRestriction(
                UUID.randomUUID(),  "main", "12", "rest", "12", null, null
        );

        CourseRestriction gradCourseRestriction = new CourseRestriction();
        gradCourseRestriction.setCourseRestrictionId(UUID.randomUUID());
        gradCourseRestriction.setMainCourse("main");
        gradCourseRestriction.setMainCourseLevel("12");
        gradCourseRestriction.setRestrictedCourse("rest");
        gradCourseRestriction.setRestrictedCourseLevel("12");

        when(this.restUtils.getCourseRestriction(eq("main"), eq("12"), eq("rest"), eq("12"), eq(summary.getAccessToken()))).thenReturn(null);
        when(this.restUtils.saveCourseRestriction(eq(gradCourseRestriction), eq(summary.getAccessToken()))).thenReturn(gradCourseRestriction);
        dataConversionService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getAddedCount()).isEqualTo(1L);
    }

    @Test
    public void testConvertCourseRestriction_whenGivenRecordExists() {
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");

        CourseRestriction courseRestriction = new CourseRestriction(
                UUID.randomUUID(), "main", "12", "rest", "12", null, null
        );

        CourseRestriction gradCourseRestriction = new CourseRestriction();
        gradCourseRestriction.setCourseRestrictionId(UUID.randomUUID());
        gradCourseRestriction.setMainCourse("main");
        gradCourseRestriction.setMainCourseLevel("12");
        gradCourseRestriction.setRestrictedCourse("rest");
        gradCourseRestriction.setRestrictedCourseLevel("12");

        when(this.restUtils.getCourseRestriction(eq("main"), eq("12"), eq("rest"), eq("12"), eq(summary.getAccessToken()))).thenReturn(gradCourseRestriction);
        when(this.restUtils.saveCourseRestriction(eq(gradCourseRestriction), eq(summary.getAccessToken()))).thenReturn(gradCourseRestriction);
        dataConversionService.convertCourseRestriction(courseRestriction, summary);
        assertThat(summary.getUpdatedCount()).isEqualTo(1L);
    }

    @Test
    public void testLoadInitialRawGradCourseRestrictionsData() {
        Object[] obj = new Object[] {
                "main", "12", "test", "12", null, null
        };
        List<Object[]> results = new ArrayList<>();
        results.add(obj);

        when(this.traxStudentRepository.loadInitialRawCourseRestrictionData()).thenReturn(results);

        var result = dataConversionService.loadInitialRawGradCourseRestrictionsData(true);
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        CourseRestriction responseCourseRestriction = result.get(0);
        assertThat(responseCourseRestriction.getMainCourse()).isEqualTo("main");
    }
}
