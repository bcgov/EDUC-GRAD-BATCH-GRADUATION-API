package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.entity.StudentCredentialDistributionEntity;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.repository.StudentCredentialDistributionRepository;
import ca.bc.gov.educ.api.batchgraduation.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DistributionService extends GradService {

    @Autowired
    JsonUtil jsonUtil;

    private final GradBatchHistoryService gradBatchHistoryService;
    private final StudentCredentialDistributionRepository studentCredentialDistributionRepository;

    public DistributionService(GradBatchHistoryService gradBatchHistoryService,
                               StudentCredentialDistributionRepository studentCredentialDistributionRepository) {
        this.gradBatchHistoryService = gradBatchHistoryService;
        this.studentCredentialDistributionRepository = studentCredentialDistributionRepository;
    }

    @Transactional(readOnly = true)
    public String getJobTypeFromBatchJobHistory(Long batchId) {
        BatchGradAlgorithmJobHistoryEntity entity = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        return entity != null? entity.getJobType() : null;
    }

    @Transactional(readOnly = true)
    public List<StudentCredentialDistribution> getStudentCredentialDistributions(Long batchId) {
        List<StudentCredentialDistributionEntity> entityList = studentCredentialDistributionRepository.findByJobExecutionId(batchId);
        return entityList.stream().map(e -> {
            try {
                return jsonUtil.getJsonObjectFromString(StudentCredentialDistribution.class, e.getPayload());
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UUID> getSchoolListForDistribution(Long batchId) {
        return studentCredentialDistributionRepository.getSchoolList(batchId);
    }

    @Transactional
    public void saveStudentCredentialDistribution(Long batchId, String jobType, StudentCredentialDistribution scd) {
        StudentCredentialDistributionEntity entity = new StudentCredentialDistributionEntity();
        entity.setJobExecutionId(batchId);
        entity.setJobType(jobType);
        entity.setStudentID(scd.getStudentID());
        entity.setSchoolId(scd.getSchoolId());
        try {
            String payload = jsonUtil.getJsonStringFromObject(scd);
            entity.setPayload(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        studentCredentialDistributionRepository.save(entity);
    }

    @Transactional
    public void updateDistributionBatchJobStatus(Long batchId, int failedCount, String status) {
        log.debug("updateDistributionBatchJobStatus - retrieve the batch job history: batchId = {}", batchId);
        BatchGradAlgorithmJobHistoryEntity jobHistory = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        if(jobHistory != null) {
            if(BatchStatusEnum.COMPLETED.name().equalsIgnoreCase(status) || BatchStatusEnum.FAILED.name().equalsIgnoreCase(status) || BatchStatusEnum.STOPPED.name().equalsIgnoreCase(status)) {
                jobHistory.setEndTime(LocalDateTime.now());
            }
            jobHistory.setStatus(status);
            jobHistory.setActualStudentsProcessed(jobHistory.getExpectedStudentsProcessed() - failedCount);
            log.debug("updateDistributionBatchJobStatus - save the batch job history: batchId = {}, status = {}. actual processed count = {}", batchId, status, jobHistory.getActualStudentsProcessed());
            gradBatchHistoryService.saveGradAlgorithmJobHistory(jobHistory);
            log.debug("updateDistributionBatchJobStatus - save the batch job history is completed!");
        }
    }

}
