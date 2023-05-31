package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.StudentCredentialDistributionEntity;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.repository.StudentCredentialDistributionRepository;
import ca.bc.gov.educ.api.batchgraduation.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class DistributionService extends GradService {

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
                return JsonUtil.getJsonObjectFromString(StudentCredentialDistribution.class, e.getPayload());
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSchoolListForDistribution(Long batchId) {
        return studentCredentialDistributionRepository.getSchoolList(batchId);
    }

    @Transactional
    public void saveStudentCredentialDistribution(Long batchId, String jobType, StudentCredentialDistribution scd) {
        StudentCredentialDistributionEntity entity = new StudentCredentialDistributionEntity();
        entity.setJobExecutionId(batchId);
        entity.setJobType(jobType);
        entity.setStudentID(scd.getStudentID());
        entity.setSchoolOfRecord(scd.getSchoolOfRecord());
        try {
            String payload = JsonUtil.getJsonStringFromObject(scd);
            entity.setPayload(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        studentCredentialDistributionRepository.save(entity);
    }

    @Transactional
    public void updateDistributionBatchJobStatus(Long batchId, int failedCount, String status, String jobParameters) {
        log.debug("updateDistributionBatchJobStatus - retrieve the batch job history: batchId = {}", batchId);
        BatchGradAlgorithmJobHistoryEntity jobHistory = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        jobHistory.setEndTime(new Date(System.currentTimeMillis()));
        jobHistory.setStatus(status);
        jobHistory.setActualStudentsProcessed(jobHistory.getExpectedStudentsProcessed() - failedCount);
        jobHistory.setJobParameters(jobParameters);
        log.debug("updateDistributionBatchJobStatus - save the batch job history: batchId = {}, status = {}. actual processed count = {}", batchId, status, jobHistory.getActualStudentsProcessed());
        gradBatchHistoryService.saveGradAlgorithmJobHistory(jobHistory);
        log.debug("updateDistributionBatchJobStatus - save the batch job history is completed!");
    }


}