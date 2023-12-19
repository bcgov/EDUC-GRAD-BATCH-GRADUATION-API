package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface BatchJobExecutionRepository extends JpaRepository<BatchJobExecutionEntity, Long> {

    Page<BatchJobExecutionEntity> findAllByOrderByCreateTimeDesc(Pageable page);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (\n" +
            "SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME <= :createDate);",
            nativeQuery = true)
    void deleteBatchParamsByCreateTimeBefore(LocalDateTime createDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (\n" +
            "SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME <= :createDate);",
            nativeQuery = true)
    void deleteBatchContextsByCreateTimeBefore(LocalDateTime createDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN (\n" +
            "SELECT JOB_INSTANCE_ID FROM BATCH_JOB_EXECUTION);",
            nativeQuery = true)
    void deleteBatchInstancesNotInBatchJobs();

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME <= :createDate;",
            nativeQuery = true)
    void deleteBatchJobsByCreateTimeBefore(LocalDateTime createDate);
}
