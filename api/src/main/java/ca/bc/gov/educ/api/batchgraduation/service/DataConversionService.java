package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvCourseRestrictionsEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentSpecialProgramEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.GradSpecialProgramEntity;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionError;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvCourseRestrictionRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvGradStudentRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvGradStudentSpecialProgramRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.GradSpecialProgramRepository;
import ca.bc.gov.educ.api.batchgraduation.util.DateConversionUtils;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class DataConversionService {

    private final ConvGradStudentRepository convGradStudentRepository;
	private final ConvCourseRestrictionRepository convCourseRestrictionRepository;
	private final ConvGradStudentSpecialProgramRepository convGradStudentSpecialProgramRepository;
	private final GradSpecialProgramRepository gradSpecialProgramRepository;
	private final RestTemplate restTemplate;
	private final RestTemplateBuilder restTemplateBuilder;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_PEN_STUDENT_API_BY_PEN_URL)
    private String getPenStudentAPIByPenURL;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_GRAD_STATUS_UPDATE_URL)
    private String updateGradStatusForStudent;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_GRAD_STUDENT_API_URL)
    private String getGradStatusForStudent;

	public DataConversionService(ConvGradStudentRepository convGradStudentRepository, ConvCourseRestrictionRepository convCourseRestrictionRepository, ConvGradStudentSpecialProgramRepository convGradStudentSpecialProgramRepository, GradSpecialProgramRepository gradSpecialProgramRepository, RestTemplate restTemplate, RestTemplateBuilder restTemplateBuilder) {
		this.convGradStudentRepository = convGradStudentRepository;
		this.convCourseRestrictionRepository = convCourseRestrictionRepository;
		this.convGradStudentSpecialProgramRepository = convGradStudentSpecialProgramRepository;
		this.gradSpecialProgramRepository = gradSpecialProgramRepository;
		this.restTemplate = restTemplate;
		this.restTemplateBuilder = restTemplateBuilder;
	}

	@Transactional(readOnly = true)
	public List<String> findAll() {
		return convGradStudentRepository.findAllStudents();
	}

	@Transactional
	public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionSummaryDTO summary) {
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		try {
			Optional<ConvGradStudentEntity> stuOptional = convGradStudentRepository.findByPen(convGradStudent.getPen());
			if (stuOptional.isPresent()) {
				ConvGradStudentEntity gradStudentEntity = stuOptional.get();
				convertStudentData(convGradStudent, gradStudentEntity, summary);
				gradStudentEntity.setUpdatedTimestamp(new Date());
				gradStudentEntity = convGradStudentRepository.save(gradStudentEntity);
				summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
				// process dependencies
				processSpecialPrograms(gradStudentEntity);
			} else {
				ConvGradStudentEntity gradStudentEntity = new ConvGradStudentEntity();
				gradStudentEntity.setPen(convGradStudent.getPen());
				// Call PEN Student API
				OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
				String accessToken = auth.getTokenValue();
				HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(accessToken);
				List<Student> students = restTemplate.exchange(String.format(getPenStudentAPIByPenURL, convGradStudent.getPen()), HttpMethod.GET, new HttpEntity<>(httpHeaders), new ParameterizedTypeReference<List<Student>>() {
				}).getBody();
				if (students == null || students.isEmpty()) {
					ConversionError error = new ConversionError();
					error.setPen(convGradStudent.getPen());
					error.setReason("PEN does not exist: PEN Student API returns empty response.");
					summary.getErrors().add(error);
					return null;
				} else {
					students.forEach(st -> {
						gradStudentEntity.setStudentID(UUID.fromString(st.getStudentID()));
						convertStudentData(convGradStudent, gradStudentEntity, summary);
						convGradStudentRepository.save(gradStudentEntity);
						summary.setAddedCount(summary.getAddedCount() + 1L);
						// process dependencies
						processSpecialPrograms(gradStudentEntity);
					});
				}
			}
			return convGradStudent;
		} catch (RestClientException re) {
			ConversionError error = new ConversionError();
			error.setPen(convGradStudent.getPen());
			error.setReason("PEN Student API is failed: " + re.getLocalizedMessage());
			summary.getErrors().add(error);
			return null;
		}
	}

	@Transactional
    public List<ConvGradStudent> loadInitialRawGradStudentData(boolean purge) {
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
	public void loadInitialRawGradCourseRestrictionsData(boolean purge) {
		if (purge) {
			convCourseRestrictionRepository.deleteAll();
			convCourseRestrictionRepository.flush();
		}
		convCourseRestrictionRepository.loadInitialRawData();
		convCourseRestrictionRepository.flush();
	}

	private void convertStudentData(ConvGradStudent student, ConvGradStudentEntity studentEntity, ConversionSummaryDTO summary) {
		studentEntity.setGpa(student.getGpa());
		studentEntity.setHonoursStanding(student.getHonoursStanding());
		determineProgram(student, summary);
		studentEntity.setProgram(student.getProgram());
		studentEntity.setProgramCompletionDate(student.getProgramCompletionDate());
		studentEntity.setSchoolOfRecord(student.getSchoolOfRecord());
		studentEntity.setSchoolAtGrad(student.getSchoolAtGrad());
		studentEntity.setRecalculateGradStatus(student.getRecalculateGradStatus());
		studentEntity.setStudentGradData(student.getStudentGradData());
		studentEntity.setStudentGrade(student.getStudentGrade());
		studentEntity.setStudentStatus(student.getStudentStatus());
	}

	private void processSpecialPrograms(ConvGradStudentEntity student) {
		if (StringUtils.equals(student.getProgram(), "2018-EN")) {
			long count = convGradStudentRepository.countFrenchImmersionCourses(student.getPen());
			if (count > 0) {
				ConvGradStudentSpecialProgramEntity entity = new ConvGradStudentSpecialProgramEntity();
				entity.setPen(student.getPen());
				entity.setStudentID(student.getStudentID());
				Optional<GradSpecialProgramEntity> specialProgramOptional = gradSpecialProgramRepository.findByProgramCodeAndSpecialProgramCode("2018-EN", "FI");
				if (specialProgramOptional.isPresent()) {
					GradSpecialProgramEntity specialProgram = specialProgramOptional.get();
					entity.setSpecialProgramID(specialProgram.getId());
					convGradStudentSpecialProgramRepository.save(entity);
				}
			}
		}
	}

	private void determineProgram(ConvGradStudent student, ConversionSummaryDTO summary) {
		switch(student.getGraduationRequestYear()) {
			case "2018":
				if (student.getSchoolOfRecord().startsWith("093")) {
					student.setProgram("2018-PF");
					summary.increment("2018-PF");
				} else {
					student.setProgram("2018-EN");
					summary.increment("2018-EN");
				}
				break;
			case "2004":
				student.setProgram("2004");
				summary.increment("2004");
				break;
			case "1996":
				student.setProgram("1996");
				summary.increment("1996");
				break;
			case "1986":
				student.setProgram("1986");
				summary.increment("1986");
				break;
			case "1950":
				if (StringUtils.equals(student.getStudentGrade(), "AD")) {
					student.setProgram("1950-EN");
					summary.increment("1950-EN");
				} else if (StringUtils.equals(student.getStudentGrade(), "AN")) {
					student.setProgram("NOPROG");
					summary.increment("NOPROG");
				} else {
					// error
				}
				break;
			case "SCCP":
				student.setProgram("SCCP");
				summary.increment("SCCP");
				break;
			default:
				break;
		}
	}
}
