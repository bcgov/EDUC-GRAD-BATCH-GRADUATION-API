package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchJobExecutionRepository extends JpaRepository<BatchJobExecutionEntity, Long> {

    Page<BatchJobExecutionEntity> findAllByOrderByCreateTimeDesc(Pageable page);

}
