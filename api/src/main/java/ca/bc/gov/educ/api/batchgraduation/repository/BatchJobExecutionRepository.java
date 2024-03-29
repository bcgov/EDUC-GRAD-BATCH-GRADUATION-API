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
    @Query(value = "DELETE FROM BatchJobExecutionParamEntity bjep WHERE bjep.jobExecutionId IN (\n" +
            "SELECT bje.jobExecutionId FROM BatchJobExecutionEntity bje WHERE bje.createTime <= :createDate)")
    void deleteBatchParamsByCreateTimeBefore(LocalDateTime createDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BatchJobExecutionContextEntity bjec WHERE bjec.jobExecutionId IN (\n" +
            "SELECT bje.jobExecutionId FROM BatchJobExecutionEntity bje WHERE bje.createTime <= :createDate)")
    void deleteBatchContextsByCreateTimeBefore(LocalDateTime createDate);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BatchJobInstanceEntity bji WHERE bji.id NOT IN (\n" +
            "SELECT bje.id FROM BatchJobExecutionEntity bje)")
    void deleteBatchInstancesNotInBatchJobs();

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM BatchJobExecutionEntity bje WHERE bje.createTime <= :createDate")
    void deleteBatchJobsByCreateTimeBefore(LocalDateTime createDate);
}
