package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.StudentCredentialDistributionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentCredentialDistributionRepository extends JpaRepository<StudentCredentialDistributionEntity, UUID> {

    List<StudentCredentialDistributionEntity> findAll();

    List<StudentCredentialDistributionEntity> findByJobExecutionId(Long batchId);

    Optional<StudentCredentialDistributionEntity> findByStudentIDAndJobExecutionId(UUID studentID, Long batchId);

    List<StudentCredentialDistributionEntity> findByJobExecutionIdAndStatusIn(Long batchId, List<String> statuses);

    Page<StudentCredentialDistributionEntity> findByJobExecutionIdAndStatusIn(Long batchId, List<String> statuses, Pageable paging);

    @Query(value="select distinct e.schoolOfRecord from StudentCredentialDistributionEntity e where e.jobExecutionId = :batchId")
    List<String> getSchoolList(Long batchId);

    long countAllByJobExecutionIdAndStatus(Long batchId, String status);

    long countAllByJobExecutionIdAndStatusIn(Long batchId, List<String> statuses);

    long countAllByJobExecutionId(Long batchId);

    @Transactional
    @Modifying
    @Query("delete from StudentCredentialDistributionEntity where createDate <= :createDate")
    void deleteByCreateDateBefore(LocalDateTime createDate);

}
