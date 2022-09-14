package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchJobTypeRepository extends JpaRepository<BatchJobTypeEntity, String> {

    List<BatchJobTypeEntity> findAll();

}
