package ca.bc.gov.educ.api.batchgraduation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;

@Repository
public interface BatchGradAlgorithmJobHistoryRepository extends JpaRepository<BatchGradAlgorithmJobHistoryEntity, UUID> {

    List<BatchGradAlgorithmJobHistoryEntity> findAll();

}
