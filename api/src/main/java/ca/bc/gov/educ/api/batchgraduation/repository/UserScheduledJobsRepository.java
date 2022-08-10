package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.UserScheduledJobsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserScheduledJobsRepository extends JpaRepository<UserScheduledJobsEntity, UUID> {

    List<UserScheduledJobsEntity> findByStatus(String status);

}
