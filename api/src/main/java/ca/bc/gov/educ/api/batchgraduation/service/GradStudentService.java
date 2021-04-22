package ca.bc.gov.educ.api.batchgraduation.service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.entity.GraduationStatusEntity;
import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.repository.GraduationStatusRepository;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;

@Service
public class GradStudentService {

    @Autowired
    GraduationStatusRepository graduationStatusRepository;
    
    @Autowired
    RestTemplate restTemplate;
    
    @Autowired
    RestTemplateBuilder restTemplateBuilder;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_PEN_STUDENT_API_BY_PEN_URL)
    private String getPenStudentAPIByPenURL;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_GRAD_STATUS_UPDATE_URL)
    private String updateGradStatusForStudent;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_GRAD_STUDENT_API_URL)
    private String getGradStatusForStudent;  
    
    
    @Transactional
    public void getStudentByPenFromStudentAPI(List<LoadStudentData> loadStudentData, String accessToken) {
    	loadStudentData.forEach(student -> {
    		HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(accessToken);
        	List<Student> stuDataList = restTemplate.exchange(String.format(getPenStudentAPIByPenURL, student.getPen()), HttpMethod.GET,
    				new HttpEntity<>(httpHeaders), new ParameterizedTypeReference<List<Student>>() {}).getBody();
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
