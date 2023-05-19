package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.entity.StudentCredentialDistributionEntity;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmStudentRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.StudentCredentialDistributionRepository;
import ca.bc.gov.educ.api.batchgraduation.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GradBatchHistoryService {

    @Autowired
    private BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

    @Autowired
    private BatchGradAlgorithmStudentRepository batchGradAlgorithmStudentRepository;

    @Autowired
    private StudentCredentialDistributionRepository studentCredentialDistributionRepository;

    @Transactional(readOnly = true)
    public BatchGradAlgorithmJobHistoryEntity getGradAlgorithmJobHistory(Long batchId) {
        Optional<BatchGradAlgorithmJobHistoryEntity> optional = batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId);
        return optional.orElse(null);
    }

    @Transactional
    public BatchGradAlgorithmJobHistoryEntity saveGradAlgorithmJobHistory(BatchGradAlgorithmJobHistoryEntity ent) {
        Optional<BatchGradAlgorithmJobHistoryEntity> optional = batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(ent.getJobExecutionId());
        if (optional.isPresent()) {
            // update
            BatchGradAlgorithmJobHistoryEntity current = optional.get();
            current.setStatus(ent.getStatus());
            current.setEndTime(ent.getEndTime());
            current.setExpectedStudentsProcessed(ent.getExpectedStudentsProcessed());
            current.setActualStudentsProcessed(ent.getActualStudentsProcessed());
            current.setFailedStudentsProcessed(ent.getFailedStudentsProcessed());
            current.setLocalDownload(ent.getLocalDownload());
            current.setJobParameters(ent.getJobParameters());
            return batchGradAlgorithmJobHistoryRepository.save(current);
        } else {
            // create
            return batchGradAlgorithmJobHistoryRepository.save(ent);
        }
    }

    @Transactional
    public void saveGradAlgorithmStudents(List<BatchGradAlgorithmStudentEntity> entList) {
        batchGradAlgorithmStudentRepository.saveAll(entList);
    }

    @Transactional
    public BatchGradAlgorithmStudentEntity saveGradAlgorithmStudent(BatchGradAlgorithmStudentEntity entity) {
        return batchGradAlgorithmStudentRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<BatchGradAlgorithmStudentEntity> getErroredStudents(Long batchId) {  // STARTED or FAILED
        return batchGradAlgorithmStudentRepository.findByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.STARTED.name(), BatchStatusEnum.FAILED.name()));
    }

    @Transactional(readOnly = true)
    public List<BatchGradAlgorithmStudentEntity> getAllStudents(Long batchId) {
        return batchGradAlgorithmStudentRepository.findByJobExecutionId(batchId);
    }

    @Transactional(readOnly = true)
    public BatchGradAlgorithmStudentEntity getBatchGradAlgorithmStudent(Long batchId, UUID studentID) {
        Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentID, batchId);
        return optional.orElse(null);
    }

    @Transactional
    public void saveBatchAlgorithmStudent(Long batchId, UUID studentID, String program, String schoolOfRecord) {
        Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentID, batchId);
        if (optional.isPresent()) {
            BatchGradAlgorithmStudentEntity currentEntity = optional.get();
            currentEntity.setProgram(program);
            currentEntity.setSchoolOfRecord(schoolOfRecord);
            batchGradAlgorithmStudentRepository.save(currentEntity);
        } else {
            BatchGradAlgorithmStudentEntity entity = new BatchGradAlgorithmStudentEntity();
            entity.setJobExecutionId(batchId);
            entity.setStudentID(studentID);
            entity.setProgram(program);
            entity.setSchoolOfRecord(schoolOfRecord);
            entity.setStatus(BatchStatusEnum.STARTED.name());
            batchGradAlgorithmStudentRepository.save(entity);
        }
    }

    @Transactional
    public void updateBatchStatusForStudent(Long batchId, UUID studentID, BatchStatusEnum batchStatus, String errorMessage) {
        Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentID, batchId);
        if (optional.isPresent()) {
            BatchGradAlgorithmStudentEntity currentEntity = optional.get();
            if (batchStatus == BatchStatusEnum.FAILED) {
                currentEntity.setError(errorMessage);
            }
            currentEntity.setStatus(batchStatus.name());
            batchGradAlgorithmStudentRepository.save(currentEntity);
        }
    }

    @Transactional(readOnly = true)
    public long getCountForErroredStudent(Long batchId) {
        return batchGradAlgorithmStudentRepository.countAllByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.FAILED.name(), BatchStatusEnum.STARTED.name()));
    }

    @Transactional(readOnly = true)
    public long getCountForReadStudent(Long batchId) {
        return batchGradAlgorithmStudentRepository.countAllByJobExecutionId(batchId);
    }

    @Transactional(readOnly = true)
    public long getCountForProcessedStudent(Long batchId) {
        return batchGradAlgorithmStudentRepository.countAllByJobExecutionIdAndStatus(batchId, BatchStatusEnum.COMPLETED.name());
    }

    @Transactional(readOnly = true)
    public List<String> getSchoolListForReport(Long batchId) {
        return batchGradAlgorithmStudentRepository.getSchoolList(batchId);
    }

    @Transactional
    public void copyAllStudentsIntoNewBatch(Long newBatchId, Long fromBatchId, String username) {
        batchGradAlgorithmStudentRepository.copyAllGradAlgorithmStudents(
                newBatchId, fromBatchId,
                username,
                new Date(System.currentTimeMillis())
        );
    }

    @Transactional
    public void copyErroredStudentsIntoNewBatch(Long newBatchId, Long fromBatchId, String username) {
        batchGradAlgorithmStudentRepository.copyGradAlgorithmErroredStudents(
                newBatchId, fromBatchId,
                username,
                new Date(System.currentTimeMillis())
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getGraduationProgramCountsForBatchRunSummary(Long batchId) {
        Map<String, Integer> response = new HashMap<>();
        List<Object[]> results = batchGradAlgorithmStudentRepository.getGraduationProgramCounts(batchId);
        if (results != null && !results.isEmpty()) {
            results.forEach(field -> {
                String program = (String) field[0];
                BigDecimal count = (BigDecimal) field[1];
                response.put(program, count.intValue());
            });
        }
        return response;
    }



    // Distribution
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
}
