package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchProcessingRepository extends JpaRepository<BatchProcessingEntity, UUID> {

    Optional<BatchProcessingEntity> findByJobType(String jobType);

}
