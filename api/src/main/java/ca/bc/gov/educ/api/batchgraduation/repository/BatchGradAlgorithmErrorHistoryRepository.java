package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchGradAlgorithmErrorHistoryRepository extends JpaRepository<BatchGradAlgorithmErrorHistoryEntity, UUID> {

    List<BatchGradAlgorithmErrorHistoryEntity> findAll();

    Page<BatchGradAlgorithmErrorHistoryEntity> findByJobExecutionId(Long batchId, Pageable paging);

    BatchGradAlgorithmErrorHistoryEntity findByStudentID(UUID studentID);
}
