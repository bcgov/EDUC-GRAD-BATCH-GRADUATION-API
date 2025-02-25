package ca.bc.gov.educ.api.batchgraduation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BatchGradAlgorithmJobHistoryRepository extends JpaRepository<BatchGradAlgorithmJobHistoryEntity, UUID>, JpaSpecificationExecutor<BatchGradAlgorithmJobHistoryEntity> {

    List<BatchGradAlgorithmJobHistoryEntity> findAll();

    Optional<BatchGradAlgorithmJobHistoryEntity> findByJobExecutionId(Long batchId);

    @Transactional
    @Modifying
    @Query("delete from BatchGradAlgorithmJobHistoryEntity where createDate <= :createDate")
    void deleteByCreateDateBefore(LocalDateTime createDate);
}
