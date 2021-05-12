package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentSpecialProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConvGradStudentSpecialProgramRepository extends JpaRepository<ConvGradStudentSpecialProgramEntity, UUID> {

}

