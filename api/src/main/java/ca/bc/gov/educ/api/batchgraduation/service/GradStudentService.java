package ca.bc.gov.educ.api.batchgraduation.service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.entity.GraduationStatusEntity;
import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.repository.GraduationStatusRepository;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;

@Service
public class GradStudentService {

    private final GraduationStatusRepository graduationStatusRepository;
	private final EducGradBatchGraduationApiConstants constants;
	private final RestUtils restUtils;

    public GradStudentService(GraduationStatusRepository graduationStatusRepository, RestUtils restUtils, EducGradBatchGraduationApiConstants constants) {
    	this.graduationStatusRepository = graduationStatusRepository;
    	this.restUtils = restUtils;
    	this.constants = constants;
	}

    @Transactional
    public void getStudentByPenFromStudentAPI(List<LoadStudentData> loadStudentData, String accessToken) {
    	loadStudentData.forEach(student -> {
        	List<Student> stuDataList = restUtils.getStudentsByPen(student.getPen(), accessToken);
        	stuDataList.forEach(st-> {
    			GraduationStatusEntity gradStu = new GraduationStatusEntity();			
    			Optional<GraduationStatusEntity> existingStu = graduationStatusRepository.findById(student.getPen());
    			if(!existingStu.isPresent()) {	
	    			gradStu.setPen(student.getPen());
					gradStu.setProgram(student.getProgramCode());
					gradStu.setGpa(student.getGpa());
					gradStu.setHonoursStanding(student.getHonoursStanding());
					gradStu.setProgramCompletionDate(student.getProgramCompletionDate() != null ? Date.valueOf(student.getProgramCompletionDate()) : null);
					gradStu.setSchoolOfRecord(student.getSchool());
					gradStu.setStudentGrade(student.getStudentGrade());
					gradStu.setStudentStatus(student.getStudentStatus());
					gradStu.setStudentID(UUID.fromString(st.getStudentID()));
	    			graduationStatusRepository.save(gradStu);      	
    			}
    		});
    	});
    	
    }
}
