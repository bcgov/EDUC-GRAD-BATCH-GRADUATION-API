package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;

@Service
public class GradStudentService {

	private final RestUtils restUtils;

    public GradStudentService(RestUtils restUtils) {
    	this.restUtils = restUtils;
	}

    @Transactional
    public void getStudentByPenFromStudentAPI(List<LoadStudentData> loadStudentData, String accessToken) {
    	loadStudentData.forEach(student -> {
        	List<Student> stuDataList = restUtils.getStudentsByPen(student.getPen(), accessToken);
        	stuDataList.forEach(st-> {
        		GraduationStudentRecord gradStu = new GraduationStudentRecord();
				gradStu.setProgram(student.getProgramCode());
				gradStu.setSchoolOfRecord(student.getSchool());
				gradStu.setStudentGrade(student.getStudentGrade());
				gradStu.setRecalculateGradStatus("Y");
				gradStu.setStudentStatus(student.getStudentStatus());
				gradStu.setStudentID(UUID.fromString(st.getStudentID()));
				restUtils.saveGraduationStudentRecord(gradStu, accessToken);    			
    		});
    	});
    	
    }
}
