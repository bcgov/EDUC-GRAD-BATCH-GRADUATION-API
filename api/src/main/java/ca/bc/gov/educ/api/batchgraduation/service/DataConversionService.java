package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvCourseRestrictionsEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionError;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvCourseRestrictionRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvGradStudentRepository;
import ca.bc.gov.educ.api.batchgraduation.util.DateConversionUtils;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataConversionService {

    @Autowired
		ConvGradStudentRepository convGradStudentRepository;

    @Autowired
		ConvCourseRestrictionRepository convCourseRestrictionRepository;

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
		public List<String> findAll() {
			return convGradStudentRepository.findAllStudents();
		}
    
    @Transactional
    public void updateStudent(String pen, String accessToken, ConversionSummaryDTO summary) {
			HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(accessToken);
			try {
				List<Student> students = restTemplate.exchange(String.format(getPenStudentAPIByPenURL, pen), HttpMethod.GET, new HttpEntity<>(httpHeaders), new ParameterizedTypeReference<List<Student>>() {
				}).getBody();

				students.forEach(st -> {
					Optional<ConvGradStudentEntity> stuOptional = convGradStudentRepository.findById(pen);
					if (stuOptional.isPresent()) {
						ConvGradStudentEntity gradStu = stuOptional.get();
						gradStu.setStudentID(UUID.fromString(st.getStudentID()));
						convGradStudentRepository.save(gradStu);
						summary.setGradStudentAddedCount(summary.getGradStudentAddedCount() + 1L);
					} else {
						ConversionError error = new ConversionError();
						error.setPen(pen);
						error.setReason("PEN does not exist: PEN Student API returns empty response.");
						summary.getErrors().add(error);
					}
				});
			} catch (RestClientException re) {
				ConversionError error = new ConversionError();
				error.setPen(pen);
				error.setReason("PEN Student API is failed: " + re.getLocalizedMessage());
				summary.getErrors().add(error);
			}
    }

		@Transactional
    public void loadInitialRawGradStudentData(boolean purge) throws Exception {
			if (purge) {
				convGradStudentRepository.deleteAll();
				convGradStudentRepository.loadInitialRawData();
			}
		}

		@Transactional
		public List<ConvCourseRestrictionsEntity> findAllCourseRestrictions() { return convCourseRestrictionRepository.findAll(); }

		@Transactional
		public void updateCourseRestrictions() {
			List<ConvCourseRestrictionsEntity> entities =  convCourseRestrictionRepository.findAllWithStartDateOrEndDate();
			entities.forEach(cr -> {
				boolean isUpdated = false;
				// data conversion
				if (StringUtils.isNotBlank(cr.getRestrictionStartDateStr())) {
					Date start = DateConversionUtils.convertStringToDate(cr.getRestrictionStartDateStr());
					if (start != null) {
						cr.setRestrictionStartDate(start);
						isUpdated = true;
					}
				} else if (StringUtils.isNotBlank(cr.getRestrictionEndDateStr())) {
					Date end = DateConversionUtils.convertStringToDate(cr.getRestrictionStartDateStr());
					if (end != null) {
						cr.setRestrictionEndDate(end);
						isUpdated = true;
					}
				}
				if (isUpdated) {
					convCourseRestrictionRepository.save(cr);
				}
			});
		}

		@Transactional
		public void loadInitialRawGradCourseRestrictionsData(boolean purge) throws Exception {
			if (purge) {
				convCourseRestrictionRepository.deleteAll();
				convCourseRestrictionRepository.loadInitialRawData();
			}
		}
}
