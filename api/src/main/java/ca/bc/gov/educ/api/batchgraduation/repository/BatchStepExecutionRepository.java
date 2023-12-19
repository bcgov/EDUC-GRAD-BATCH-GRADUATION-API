package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchStepExecutionRepository extends JpaRepository<BatchStepExecutionEntity, Long> {

    List<BatchStepExecutionEntity> findByJobExecutionIdOrderByEndTimeDesc(Long jobExecutionId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (\n" +
            "SELECT BATCH_STEP_EXECUTION.STEP_EXECUTION_ID FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (\n" +
            "    SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME <= :createDate\n" +
            "));\n",
            nativeQuery = true)
    void deleteBatchStepContextsByCreateTimeBefore(LocalDateTime createDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (\n" +
            "    SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION WHERE CREATE_TIME <= :createDate);",
            nativeQuery = true)
    void deleteBatchStepsByCreateTimeBefore(LocalDateTime createDate);

}
