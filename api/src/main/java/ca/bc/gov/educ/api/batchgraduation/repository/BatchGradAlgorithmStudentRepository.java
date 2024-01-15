package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface BatchGradAlgorithmStudentRepository extends JpaRepository<BatchGradAlgorithmStudentEntity, UUID> {

    List<BatchGradAlgorithmStudentEntity> findAll();

    List<BatchGradAlgorithmStudentEntity> findByJobExecutionId(Long batchId);

    Optional<BatchGradAlgorithmStudentEntity> findByStudentIDAndJobExecutionId(UUID studentID, Long batchId);

    List<BatchGradAlgorithmStudentEntity> findByJobExecutionIdAndStatusIn(Long batchId, List<String> statuses);

    Page<BatchGradAlgorithmStudentEntity> findByJobExecutionIdAndStatusIn(Long batchId, List<String> statuses, Pageable paging);

    @Query(value="select distinct e.schoolOfRecord from BatchGradAlgorithmStudentEntity e where e.jobExecutionId = :batchId and e.status = 'COMPLETED'")
    List<String> getSchoolList(Long batchId);

    long countAllByJobExecutionIdAndStatus(Long batchId, String status);

    long countAllByJobExecutionIdAndStatusIn(Long batchId, List<String> statuses);

    long countAllByJobExecutionId(Long batchId);

    @Modifying
    @Query(value="insert into BATCH_GRAD_ALG_STUDENT(graduation_student_record_id, job_execution_id, graduation_program_code, school_of_record, status, create_user, create_date, update_user, update_date)\n"
            + "select graduation_student_record_id, :newBatchId, graduation_program_code, school_of_record, 'STARTED', :userName, :currentTime, :userName, :currentTime\n"
            + "from BATCH_GRAD_ALG_STUDENT where job_execution_id = :batchId and status <> 'COMPLETED'", nativeQuery=true)
    void copyGradAlgorithmErroredStudents(
            @Param("newBatchId") Long newBatchId,
            @Param("batchId") Long batchId,
            @Param("userName") String userName,
            @Param("currentTime") Date currentTime);

    @Modifying
    @Query(value="insert into BATCH_GRAD_ALG_STUDENT(graduation_student_record_id, job_execution_id, graduation_program_code, school_of_record, status, create_user, create_date, update_user, update_date)\n"
            + "select graduation_student_record_id, :newBatchId, graduation_program_code, school_of_record, 'STARTED', :userName, :currentTime, :userName, :currentTime\n"
            + "from BATCH_GRAD_ALG_STUDENT where job_execution_id = :batchId", nativeQuery=true)
    void copyAllGradAlgorithmStudents(
            @Param("newBatchId") Long newBatchId,
            @Param("batchId") Long batchId,
            @Param("userName") String userName,
            @Param("currentTime") Date currentTime);

    @Query(value="select graduation_program_code, count(*) from BATCH_GRAD_ALG_STUDENT\n" +
            "where job_execution_id = :batchId\n" +
            "and status = 'COMPLETED'\n" +
            "group by graduation_program_code\n" +
            "order by graduation_program_code desc", nativeQuery = true)
    List<Object[]> getGraduationProgramCounts(@Param("batchId") Long batchId);

    @Transactional
    @Modifying
    @Query("delete from BatchGradAlgorithmStudentEntity where createDate <= :createDate")
    void deleteByCreateDateBefore(LocalDateTime createDate);
}
