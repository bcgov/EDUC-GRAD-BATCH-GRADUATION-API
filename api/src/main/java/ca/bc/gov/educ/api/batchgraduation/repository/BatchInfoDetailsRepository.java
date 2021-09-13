package ca.bc.gov.educ.api.batchgraduation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchInfoDetailsEntity;

@Repository
public interface BatchInfoDetailsRepository extends JpaRepository<BatchInfoDetailsEntity, Integer> {

    List<BatchInfoDetailsEntity> findAll();

}
