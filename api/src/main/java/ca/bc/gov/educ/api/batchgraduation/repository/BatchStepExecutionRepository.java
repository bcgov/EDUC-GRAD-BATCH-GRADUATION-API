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
    @Query(value = "DELETE FROM BatchStepExecutionContextEntity bsec WHERE bsec.stepExecutionId IN (\n" +
            "SELECT bse.stepExecutionId FROM BatchStepExecutionEntity bse WHERE bse.jobExecutionId IN (\n" +
            "    SELECT bje.jobExecutionId FROM BatchJobExecutionEntity bje WHERE bje.createTime <= :createDate))")
    void deleteBatchStepContextsByCreateTimeBefore(LocalDateTime createDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BatchStepExecutionEntity bse WHERE bse.jobExecutionId IN (\n" +
            "    SELECT bje.jobExecutionId FROM BatchJobExecutionEntity bje WHERE bje.createTime <= :createDate)")
    void deleteBatchStepsByCreateTimeBefore(LocalDateTime createDate);

}
