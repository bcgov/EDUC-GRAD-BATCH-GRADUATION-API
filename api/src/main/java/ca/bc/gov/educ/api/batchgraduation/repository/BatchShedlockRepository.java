package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchShedlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BatchShedlockRepository extends JpaRepository<BatchShedlockEntity, String>, JpaSpecificationExecutor<BatchShedlockEntity> {

}