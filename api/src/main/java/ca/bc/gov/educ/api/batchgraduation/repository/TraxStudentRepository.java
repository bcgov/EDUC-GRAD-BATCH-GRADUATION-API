package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.TraxStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TraxStudentRepository extends JpaRepository<TraxStudentEntity, String>  {

	@Query(value="select trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRAD, m.stud_grade as STUD_GRADE, m.stud_status as STUD_STATUS,\n" +
			"m.grad_reqt_year as GRAD_REQT_YEAR, 'Y' as RECALCULATE_GRAD_STATUS\n" +
			"from trax_students_load l, student_master m\n" +
			"where 1 = 1\n" +
			"and l.stud_no = m.stud_no\n" +
			"and m.grad_date = 0\n" +
			"and m.archive_flag = 'A'\n" , nativeQuery=true)
	@Transactional(readOnly = true)
	List<Object[]> loadInitialRawData();

	@Query(value="select trim(c1.crse_code) as CRSE_MAIN, trim(c1.crse_level) as CRSE_MAIN_LVL,\n" +
			" trim(c2.crse_code) as CRSE_RESTRICTED, trim(c2.crse_level) as CRSE_RESTRICTED_LVL,\n" +
			" trim(c1.start_restrict_session) as RESTRICTION_START_DT, trim(c1.end_restrict_session) as RESTRICTION_END_DT\n" +
			"from tab_crse c1\n" +
			"join tab_crse c2\n" +
			"on c1.restriction_code = c2.restriction_code\n" +
			"and (c1.crse_code  <> c2.crse_code or c1.crse_level <> c2.crse_level)\n" +
			"and c1.restriction_code <> ' '", nativeQuery=true)
	@Transactional(readOnly = true)
	List<Object[]> loadInitialRawCourseRestrictionData();

}
