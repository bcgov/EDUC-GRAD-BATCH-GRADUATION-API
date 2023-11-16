package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchStepExecutionRepository extends JpaRepository<BatchStepExecutionEntity, Long> {

    List<BatchStepExecutionEntity> findByJobExecutionIdOrderByEndTimeDesc(Long jobExecutionId);

}
