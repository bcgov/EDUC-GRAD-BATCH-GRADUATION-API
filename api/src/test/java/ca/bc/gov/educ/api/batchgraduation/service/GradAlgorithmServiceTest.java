package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmResponse;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ProcessError;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradAlgorithmServiceTest {

    @Autowired
    GradAlgorithmService gradAlgorithmService;

    @MockBean
    RestUtils restUtils;

    @Test
    public void testProcessStudent() {
        GraduationStudentRecord item = new GraduationStudentRecord();
        item.setStudentID(UUID.randomUUID());
        item.setPen("123456789");
        item.setProgramCompletionDate(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");
        summary.setBatchId(121L);

        AlgorithmResponse response = new AlgorithmResponse();
        response.setGraduationStudentRecord(item);

        when(restUtils.runGradAlgorithm(eq(item.getStudentID()), eq(summary.getAccessToken()),eq(item.getProgramCompletionDate()),eq(summary.getBatchId()))).thenReturn(response);
        var result =  gradAlgorithmService.processStudent(item, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(item.getStudentID());
    }

    @Test
    public void testProjectedGradProcess() {
        GraduationStudentRecord item = new GraduationStudentRecord();
        item.setStudentID(UUID.randomUUID());
        item.setPen("123456789");
        item.setProgramCompletionDate(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");
        summary.setBatchId(121L);

        AlgorithmResponse response = new AlgorithmResponse();
        response.setGraduationStudentRecord(item);

        when(restUtils.runProjectedGradAlgorithm(eq(item.getStudentID()), eq(summary.getAccessToken()),eq(summary.getBatchId()))).thenReturn(response);
        var result =  gradAlgorithmService.processProjectedGradStudent(item, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(item.getStudentID());
    }

    @Test
    public void testProcessStudent_whenExceptionOccurs_thenReturnNullWithErrorsInSummary() {
        GraduationStudentRecord item = new GraduationStudentRecord();
        item.setStudentID(UUID.randomUUID());
        item.setPen("123456789");

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");
        summary.setBatchId(123L);

        AlgorithmResponse response = new AlgorithmResponse();
        response.setGraduationStudentRecord(item);

        when(restUtils.runGradAlgorithm(item.getStudentID(), eq(any(String.class)),eq(item.getProgramCompletionDate()),eq(summary.getBatchId()))).thenThrow(new RuntimeException("Unexpected Exception is occurred!"));
        var result =  gradAlgorithmService.processStudent(item, summary);
        assertThat(result).isNull();
        assertThat(summary.getErrors().isEmpty()).isFalse();
        ProcessError error = summary.getErrors().get(0);
        assertThat(error.getStudentID()).isEqualTo(item.getStudentID().toString());
        assertThat(error.getReason().startsWith("Unexpected Exception is occurred:")).isFalse();
    }

    @Test
    public void testProjectedGradProcess_whenExceptionOccurs_thenReturnNullWithErrorsInSummary() {
        GraduationStudentRecord item = new GraduationStudentRecord();
        item.setStudentID(UUID.randomUUID());
        item.setPen("123456789");

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");
        summary.setBatchId(123L);

        AlgorithmResponse response = new AlgorithmResponse();
        response.setGraduationStudentRecord(item);

        when(restUtils.runProjectedGradAlgorithm(item.getStudentID(), eq(any(String.class)),eq(summary.getBatchId()))).thenThrow(new RuntimeException("Unexpected Exception is occurred!"));
        var result =  gradAlgorithmService.processProjectedGradStudent(item, summary);
        assertThat(result).isNull();
        assertThat(summary.getErrors().isEmpty()).isFalse();
        ProcessError error = summary.getErrors().get(0);
        assertThat(error.getStudentID()).isEqualTo(item.getStudentID().toString());
    }
}
