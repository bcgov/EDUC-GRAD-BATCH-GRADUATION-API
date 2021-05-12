package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.GradSpecialProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradSpecialProgramRepository extends JpaRepository<GradSpecialProgramEntity, UUID> {

    Optional<GradSpecialProgramEntity> findByProgramCodeAndSpecialProgramCode(String programCode, String specialProgramCode);
}
