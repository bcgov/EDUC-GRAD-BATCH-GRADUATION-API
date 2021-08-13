package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradStudentServiceTest {

    @Autowired
    GradStudentService gradStudentService;

    @MockBean
    private RestUtils restUtils;

    @Test
    public void testGetStudentByPenFromStudentAPI() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        LoadStudentData loadStudentData = new LoadStudentData();
        loadStudentData.setPen(pen);

        Student student = new Student();
        student.setPen(pen);
        student.setStudentID(studentID.toString());

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        when(this.restUtils.getStudentsByPen(eq(pen), eq("accessToken"))).thenReturn(Arrays.asList(student));
        when(this.restUtils.saveGraduationStudentRecord(eq(graduationStatus), any(String.class))).thenReturn(graduationStatus);

        gradStudentService.getStudentByPenFromStudentAPI(Arrays.asList(loadStudentData),"accessToken");
        Mockito.verify(this.restUtils).saveGraduationStudentRecord(any(GraduationStudentRecord.class), any(String.class));
    }

}
