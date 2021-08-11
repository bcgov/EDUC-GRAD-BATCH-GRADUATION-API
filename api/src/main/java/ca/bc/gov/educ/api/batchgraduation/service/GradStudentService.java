package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.Student;

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
				GraduationStatus gradStu = restUtils.getGraduationStatus(student.getPen(), accessToken);
    			if(gradStu != null && gradStu.getStudentID() != null) {
	    			gradStu.setPen(student.getPen());
					gradStu.setProgram(student.getProgramCode());
					gradStu.setGpa(student.getGpa());
					gradStu.setHonoursStanding(student.getHonoursStanding());
					gradStu.setProgramCompletionDate(student.getProgramCompletionDate());
					gradStu.setSchoolOfRecord(student.getSchool());
					gradStu.setStudentGrade(student.getStudentGrade());
					gradStu.setStudentStatus(student.getStudentStatus());
					gradStu.setStudentID(UUID.fromString(st.getStudentID()));
					restUtils.saveGraduationStatus(gradStu, accessToken);
    			}
    		});
    	});
    	
    }
}
