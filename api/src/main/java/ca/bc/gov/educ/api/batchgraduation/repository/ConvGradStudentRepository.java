package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ConvGradStudentRepository extends JpaRepository<ConvGradStudentEntity, String> {

	List<ConvGradStudentEntity> findAll();

	@Query(value="select pen from conv_grad_student where student_id is null", nativeQuery=true)
	List<String> findAllStudents();

	@Modifying
	@Query(value="insert into CONV_GRAD_STUDENT(pen, school_of_record, school_at_grad, stud_grade, FK_GRAD_STUDENT_STUDENT_STATUS, FK_GRAD_PROGRAM_CODE, RECALCULATE_GRAD_STATUS)\n" +
					"select  trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRAD, m.stud_grade as STUD_GRADE, m.stud_status as STUD_STATUS, '2018-EN', 'Y'\n" +
					"from trax_students_load l, student_master m\n" +
					"where 1 = 1\n" +
					"and l.stud_no = m.stud_no\n" +
					"and m.grad_reqt_year = 2018\n" +
					"and m.grad_date = 0\n" +
					"and m.archive_flag = 'A'\n" +
					"and m.mincode not like '093%'\n", nativeQuery=true)
	@Transactional
	void loadInitialRawData();

}
