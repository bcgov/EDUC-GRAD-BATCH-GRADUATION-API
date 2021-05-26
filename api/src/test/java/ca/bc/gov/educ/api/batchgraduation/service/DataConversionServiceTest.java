package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvGradStudentRepository;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.GradBatchTestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.RestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@ActiveProfiles("test")
public class DataConversionServiceTest {

    @Mock(answer = CALLS_REAL_METHODS)
    DataConversionService dataConversionService;

    @Autowired
    ConvGradStudentRepository convGradStudentRepository;

    @MockBean
    RestUtils restUtils;

    @Autowired
    private EducGradBatchGraduationApiConstants constants;

    @Autowired
    GradBatchTestUtils gradBatchTestUtils;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {
        convGradStudentRepository.deleteAll();
    }

//    @Test
    public void convertStudent_whenGivenData_withoutSpecialProgram_thenReturnSuccess() throws Exception {
        gradBatchTestUtils.createConvGradStudents("mock_conv_grad_students.json");

        List<ConvGradStudentEntity> entities = convGradStudentRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(2);

        UUID studentID = UUID.randomUUID();
        Student penStudent = new Student();
        penStudent.setStudentID(studentID.toString());
        penStudent.setPen("111222333");
        when(this.restUtils.getStudentsByPen("111222333", "123")).thenReturn(Arrays.asList(penStudent));
        when(this.dataConversionService.isFrenchImmersionCourse(any())).thenReturn(false);

        ConvGradStudent student = ConvGradStudent.builder().pen("111222333").program("2018-PF").recalculateGradStatus("N")
                .studentStatus("A").schoolOfRecord("222333").graduationRequestYear("2018").build();
        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setAccessToken("123");
        dataConversionService.convertStudent(student, summary);

        entities = convGradStudentRepository.findAll();
        assertThat(entities).isNotNull();
        assertThat(entities.size()).isEqualTo(3);

        Optional<ConvGradStudentEntity> result = convGradStudentRepository.findByPen("111222333");
        assertThat(result).isNotNull();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getStudentID()).isEqualTo(studentID.toString());
        assertThat(result.get().getRecalculateGradStatus()).isEqualTo("Y");
    }

}
