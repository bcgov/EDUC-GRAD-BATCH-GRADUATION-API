package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvCourseRestrictionsEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

		@Transactional(readOnly = true)
		public List<String> findAll() {
			return convGradStudentRepository.findAllStudents();
		}
    
    @Transactional
    public void convertGradStudent(ConvGradStudent convGradStudent, String accessToken, ConversionSummaryDTO summary) {
			HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(accessToken);
			try {
				Optional<ConvGradStudentEntity> stuOptional = convGradStudentRepository.findById(convGradStudent.getPen());
				if (stuOptional.isPresent()) {
					ConvGradStudentEntity gradStudentEntity = stuOptional.get();
					populate(convGradStudent, gradStudentEntity);
					gradStudentEntity.setUpdatedTimestamp(new Date());
					convGradStudentRepository.save(gradStudentEntity);
					summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
				} else {
					ConvGradStudentEntity gradStudentEntity = new ConvGradStudentEntity();
					gradStudentEntity.setPen(convGradStudent.getPen());
					// Call PEN Student API
					List<Student> students = restTemplate.exchange(String.format(getPenStudentAPIByPenURL, convGradStudent.getPen()), HttpMethod.GET, new HttpEntity<>(httpHeaders), new ParameterizedTypeReference<List<Student>>() {
					}).getBody();
					if (students.isEmpty()) {
						ConversionError error = new ConversionError();
						error.setPen(convGradStudent.getPen());
						error.setReason("PEN does not exist: PEN Student API returns empty response.");
						summary.getErrors().add(error);
					} else {
						students.forEach(st -> {
							gradStudentEntity.setStudentID(UUID.fromString(st.getStudentID()));
							populate(convGradStudent, gradStudentEntity);
							convGradStudentRepository.save(gradStudentEntity);
							summary.setAddedCount(summary.getAddedCount() + 1L);
						});
					}
				}
			} catch (RestClientException re) {
				ConversionError error = new ConversionError();
				error.setPen(convGradStudent.getPen());
				error.setReason("PEN Student API is failed: " + re.getLocalizedMessage());
				summary.getErrors().add(error);
			}
    }

		@Transactional
    public List<ConvGradStudent> loadInitialRawGradStudentData(boolean purge) throws Exception {
			if (purge) {
				convGradStudentRepository.deleteAll();
				convGradStudentRepository.flush();
			}
			List<ConvGradStudent> students = new ArrayList<>();
			List<Object[]> results = convGradStudentRepository.loadInitialRawData();
			results.forEach(result -> {
				String pen = (String) result[0];
				String schoolOfRecord = (String) result[1];
				String schoolAtGrad = (String) result[2];
				String studentGrade = (String) result[3];
				Character studentStatus = (Character) result[4];
				String graduationRequestYear = (String) result[5];
				Character recalculateGradStatus = (Character) result[6];
				ConvGradStudent student = new ConvGradStudent(
					pen, null, null, null, null,
					recalculateGradStatus.toString(), null, schoolOfRecord, schoolAtGrad, studentGrade,
					studentStatus != null? studentStatus.toString() : null, graduationRequestYear);
				students.add(student);
			});

			return students;
		}

		@Transactional(readOnly = true)
		public List<ConvCourseRestrictionsEntity> findAllCourseRestrictions() { return convCourseRestrictionRepository.findAll(); }

		@Transactional
		public void updateCourseRestrictions(ConversionSummaryDTO summary) {
			List<ConvCourseRestrictionsEntity> entities =  convCourseRestrictionRepository.findAll();
			summary.setReadCount(entities.size());
			entities.forEach(cr -> {
				summary.setProcessedCount(summary.getProcessedCount() + 1L);
				summary.setAddedCount(summary.getAddedCount() + 1L);
				boolean isUpdated = false;
				// data conversion
				if (StringUtils.isNotBlank(cr.getRestrictionStartDateStr())) {
					Date start = DateConversionUtils.convertStringToDate(cr.getRestrictionStartDateStr());
					if (start != null) {
						cr.setRestrictionStartDate(start);
						isUpdated = true;
					}
				}
				if (StringUtils.isNotBlank(cr.getRestrictionEndDateStr())) {
					Date end = DateConversionUtils.convertStringToDate(cr.getRestrictionEndDateStr());
					if (end != null) {
						cr.setRestrictionEndDate(end);
						isUpdated = true;
					}
				}
				if (isUpdated) {
					convCourseRestrictionRepository.save(cr);
				}
			});
			convCourseRestrictionRepository.flush();
		}

		@Transactional
		public void loadInitialRawGradCourseRestrictionsData(boolean purge) throws Exception {
			if (purge) {
				convCourseRestrictionRepository.deleteAll();
				convCourseRestrictionRepository.flush();
			}
			convCourseRestrictionRepository.loadInitialRawData();
			convCourseRestrictionRepository.flush();
		}

		private void populate(ConvGradStudent student, ConvGradStudentEntity studentEntity) {
			studentEntity.setGpa(student.getGpa());
			studentEntity.setHonoursStanding(student.getHonoursStanding());
			determineProgram(student);
			studentEntity.setProgram(student.getProgram());
			studentEntity.setProgramCompletionDate(student.getProgramCompletionDate());
			studentEntity.setSchoolOfRecord(student.getSchoolOfRecord());
			studentEntity.setSchoolAtGrad(student.getSchoolAtGrad());
			studentEntity.setRecalculateGradStatus(student.getRecalculateGradStatus());
			studentEntity.setStudentGradData(student.getStudentGradData());
			studentEntity.setStudentGrade(student.getStudentGrade());
			studentEntity.setStudentStatus(student.getStudentStatus());
		}

		private void determineProgram(ConvGradStudent student) {
			switch(student.getGraduationRequestYear()) {
				case "2018":
					if (student.getSchoolOfRecord().startsWith("093")) {
						student.setProgram("2018-PF");
					} else {
						student.setProgram("2018-EN");
					}
					break;
				case "2004":
					student.setProgram("2004");
					break;
				case "1996":
					student.setProgram("1996");
					break;
				case "1986":
					student.setProgram("1986");
					break;
				case "1950":
					if (StringUtils.equals(student.getStudentGrade(), "AD")) {
						student.setProgram("1950-EN");
					} else if (StringUtils.equals(student.getStudentGrade(), "AN")) {
						student.setProgram("NOPROG");
					} else {
						// error
					}
					break;
				case "SCCP":
					student.setProgram("SCCP");
					break;
				default:
					break;
			}
		}
}
