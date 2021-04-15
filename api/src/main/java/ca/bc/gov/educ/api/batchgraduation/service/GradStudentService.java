package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.entity.GraduationStatusEntity;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
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
    public void getStudentByPenFromStudentAPI(String pen, String accessToken) {
    	HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(accessToken);
    	List<Student> stuDataList = restTemplate.exchange(String.format(getPenStudentAPIByPenURL, pen), HttpMethod.GET,
				new HttpEntity<>(httpHeaders), new ParameterizedTypeReference<List<Student>>() {}).getBody();
    	stuDataList.forEach(st-> {
			GraduationStatusEntity gradStu = new GraduationStatusEntity();			
    		ResponseEntity<GraduationStatus> responseEntity = restTemplate.exchange(String.format(getGradStatusForStudent,st.getPen()), HttpMethod.GET,
				new HttpEntity<>(httpHeaders), GraduationStatus.class);
    		if(responseEntity.getStatusCode().equals(HttpStatus.OK)) {
    			BeanUtils.copyProperties(responseEntity.getBody(), gradStu);
    		}
    		restTemplate.exchange(String.format(updateGradStatusForStudent,pen), HttpMethod.POST,
				new HttpEntity<>(gradStu,httpHeaders), GraduationStatus.class).getBody();
    		
		});
    }
}
