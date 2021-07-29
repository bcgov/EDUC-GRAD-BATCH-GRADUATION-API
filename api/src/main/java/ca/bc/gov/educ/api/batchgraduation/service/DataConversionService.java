package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvGradStudentRepository;
import ca.bc.gov.educ.api.batchgraduation.util.DateConversionUtils;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DataConversionService {

    private final ConvGradStudentRepository convGradStudentRepository;
	private final RestUtils restUtils;

	@Autowired
	public DataConversionService(ConvGradStudentRepository convGradStudentRepository, RestUtils restUtils) {
		this.convGradStudentRepository = convGradStudentRepository;
		this.restUtils = restUtils;
	}

	@Transactional
	public ConvGradStudent convertStudent(ConvGradStudent convGradStudent, ConversionSummaryDTO summary) {
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		try {
			String accessToken = summary.getAccessToken();

			GraduationStatus graduationStatus = restUtils.getGraduationStatus(convGradStudent.getPen(), accessToken);
			if (graduationStatus != null && graduationStatus.getStudentID() != null) {
				convertStudentData(convGradStudent, graduationStatus, summary);
				graduationStatus.setUpdatedTimestamp(new Date());
				GraduationStatus studentResponse = restUtils.saveGraduationStatus(graduationStatus, accessToken);
				summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
				// process dependencies
				try {
					processSpecialPrograms(studentResponse, accessToken);
				} catch (Exception e) {
					ConversionError error = new ConversionError();
					error.setPen(convGradStudent.getPen());
					error.setReason("Grad Program Management API is failed: " + e.getLocalizedMessage());
					summary.getErrors().add(error);
				}
			} else {
				List<Student> students;
				try {
					// Call PEN Student API
					students = restUtils.getStudentsByPen(convGradStudent.getPen(), accessToken);
				} catch (Exception e) {
					ConversionError error = new ConversionError();
					error.setPen(convGradStudent.getPen());
					error.setReason("PEN Student API is failed: " + e.getLocalizedMessage());
					summary.getErrors().add(error);
					return null;
				}
				if (students == null || students.isEmpty()) {
					ConversionError error = new ConversionError();
					error.setPen(convGradStudent.getPen());
					error.setReason("PEN does not exist: PEN Student API returns empty response.");
					summary.getErrors().add(error);
					return null;
				} else {
					students.forEach(st -> {
						GraduationStatus newGraduationStatus = new GraduationStatus();
						newGraduationStatus.setPen(convGradStudent.getPen());
						newGraduationStatus.setStudentID(UUID.fromString(st.getStudentID()));
						convertStudentData(convGradStudent, newGraduationStatus, summary);
						GraduationStatus studentResponse = restUtils.saveGraduationStatus(newGraduationStatus, accessToken);
						summary.setAddedCount(summary.getAddedCount() + 1L);
						// process dependencies
						try {
							processSpecialPrograms(studentResponse, accessToken);
						} catch (Exception e) {
							ConversionError error = new ConversionError();
							error.setPen(convGradStudent.getPen());
							error.setReason("Grad Program Management API is failed: " + e.getLocalizedMessage());
							summary.getErrors().add(error);
						}
					});
				}
			}
			return convGradStudent;
		} catch (Exception e) {
			ConversionError error = new ConversionError();
			error.setPen(convGradStudent.getPen());
			error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
			summary.getErrors().add(error);
			return null;
		}
	}

	@Transactional
    public List<ConvGradStudent> loadInitialRawGradStudentData(boolean purge) {
		if (purge) {
//			dataConversionRepository.deleteAll();
//			dataConversionRepository.flush();
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

	@Transactional
	public void convertCourseRestriction(GradCourseRestriction courseRestriction, ConversionSummaryDTO summary) {
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		GradCourseRestriction currentCourseRestriction =  restUtils.getCourseRestriction(
			courseRestriction.getMainCourse(), courseRestriction.getMainCourseLevel(), courseRestriction.getRestrictedCourse(), courseRestriction.getRestrictedCourseLevel(), summary.getAccessToken());

		if (currentCourseRestriction == null) {
			currentCourseRestriction = new GradCourseRestriction();
		}
		convertCourseRestrictionData(courseRestriction, currentCourseRestriction);
		restUtils.saveCourseRestriction(currentCourseRestriction, summary.getAccessToken());
		if (currentCourseRestriction.getCourseRestrictionId() != null) {
			summary.setUpdatedCount(summary.getUpdatedCount() + 1L);
		} else {
			summary.setAddedCount(summary.getAddedCount() + 1L);
		}
	}

	@Transactional
	public List<GradCourseRestriction> loadInitialRawGradCourseRestrictionsData(boolean purge) {
		if (purge) {
//			gradCourseRestrictionRepository.deleteAll();
//			gradCourseRestrictionRepository.flush();
		}
		List<GradCourseRestriction> courseRestrictions = new ArrayList<>();
		List<Object[]> results = convGradStudentRepository.loadInitialRawCourseRestrictionData();
		results.forEach(result -> {
			String mainCourse = (String) result[0];
			String mainCourseLevel = (String) result[1];
			String restrictedCourse = (String) result[2];
			String restrictedCourseLevel = (String) result[3];
			String startDate = (String) result[4];
			String endDate = (String) result[5];
			GradCourseRestriction courseRestriction = new GradCourseRestriction(
					null, mainCourse, mainCourseLevel, restrictedCourse, restrictedCourseLevel, startDate, endDate);
			courseRestrictions.add(courseRestriction);
		});
		return courseRestrictions;
	}

	@Transactional
	public void removeGradCourseRestriction(String mainCourseCode, String restrictedCourseCode, ConversionSummaryDTO summary) {
		List<GradCourseRestriction> removalList = restUtils.getCourseRestrictions(mainCourseCode, restrictedCourseCode, summary.getAccessToken());
		removalList.forEach(c -> {
			// TODO (jsung) : rest api call to delete course restriction
			//gradCourseRestrictionRepository.delete(c);
			summary.setAddedCount(summary.getAddedCount() - 1L);
		});
	}

	private void convertCourseRestrictionData(GradCourseRestriction courseRestriction, GradCourseRestriction courseRestrictionEntity) {
		courseRestrictionEntity.setMainCourse(courseRestriction.getMainCourse());
		courseRestrictionEntity.setMainCourseLevel(courseRestriction.getMainCourseLevel());
		courseRestrictionEntity.setRestrictedCourse(courseRestriction.getRestrictedCourse());
		courseRestrictionEntity.setRestrictedCourseLevel(courseRestriction.getRestrictedCourseLevel());
		// data conversion
		if (StringUtils.isNotBlank(courseRestriction.getRestrictionStartDate())) {
			courseRestrictionEntity.setRestrictionStartDate(courseRestriction.getRestrictionStartDate());
//			Date start = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionStartDate());
//			if (start != null) {
//				courseRestrictionEntity.setRestrictionStartDate(start);
//			}
		}
		if (StringUtils.isNotBlank(courseRestriction.getRestrictionEndDate())) {
			courseRestrictionEntity.setRestrictionEndDate(courseRestriction.getRestrictionEndDate());
//			Date end = DateConversionUtils.convertStringToDate(courseRestriction.getRestrictionEndDate());
//			if (end != null) {
//				courseRestrictionEntity.setRestrictionEndDate(end);
//			}
		}
	}

	private void convertStudentData(ConvGradStudent convStudent, GraduationStatus student, ConversionSummaryDTO summary) {
		student.setGpa(convStudent.getGpa());
		student.setHonoursStanding(convStudent.getHonoursStanding());
		determineProgram(convStudent, summary);
		student.setProgram(convStudent.getProgram());
		student.setProgramCompletionDate(convStudent.getProgramCompletionDate() != null? DateConversionUtils.formatDate(convStudent.getProgramCompletionDate(),"yyyy/MM") : null);
		student.setSchoolOfRecord(convStudent.getSchoolOfRecord());
		student.setSchoolAtGrad(convStudent.getSchoolAtGrad());
		student.setRecalculateGradStatus(convStudent.getRecalculateGradStatus());
		student.setStudentGradData(convStudent.getStudentGradData());
		student.setStudentGrade(convStudent.getStudentGrade());
		student.setStudentStatus(convStudent.getStudentStatus());
	}

	private void processSpecialPrograms(GraduationStatus student, String accessToken) {
		// French Immersion for 2018-EN
		if (StringUtils.equals(student.getProgram(), "2018-EN")) {
			if (isFrenchImmersionCourse(student.getPen())) {
				GradStudentSpecialProgram entity = new GradStudentSpecialProgram();
				entity.setPen(student.getPen());
				entity.setId(student.getStudentID());
				// Call Grad Program Management API
				GradSpecialProgram gradSpecialProgram = restUtils.getGradSpecialProgram("2018-EN", "FI", accessToken);
				if (gradSpecialProgram != null && gradSpecialProgram.getId() != null) {
					entity.setSpecialProgramID(gradSpecialProgram.getId());
					GradStudentSpecialProgram studentSpecialProgramResponse = restUtils.getStudentSpecialProgram(student.getStudentID(), gradSpecialProgram.getId(), accessToken);
					if (studentSpecialProgramResponse == null) {
						restUtils.saveStudentSpecialProgram(entity, accessToken);
					}
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
					student.setProgram("1950");
					summary.increment("1950");
				} else if (StringUtils.equals(student.getStudentGrade(), "AN")) {
					student.setProgram("NOPROG");
					summary.increment("NOPROG");
				} else {
					// error
					ConversionError error = new ConversionError();
					error.setPen(student.getPen());
					error.setReason("Program is not found for 1950 / " + student.getStudentGrade());
					summary.getErrors().add(error);
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

	@Transactional(readOnly = true)
	public boolean isFrenchImmersionCourse(String pen) {
		if (this.restUtils.getCountOfFrenchImmersionCourses(pen) > 0L) {
		//if (this.dataConversionRepository.countFrenchImmersionCourses(pen) > 0L) {
			return true;
		}
		return false;
	}
}
