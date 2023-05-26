package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.entity.StudentCredentialDistributionEntity;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.JobParametersForDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.repository.StudentCredentialDistributionRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DistributionService extends GradService {

    private final GradBatchHistoryService gradBatchHistoryService;
    private final StudentCredentialDistributionRepository studentCredentialDistributionRepository;
    private final RestUtils restUtils;

    public DistributionService(GradBatchHistoryService gradBatchHistoryService,
                               StudentCredentialDistributionRepository studentCredentialDistributionRepository,
                                RestUtils restUtils) {
        this.gradBatchHistoryService = gradBatchHistoryService;
        this.studentCredentialDistributionRepository = studentCredentialDistributionRepository;
        this.restUtils = restUtils;
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

    @Async("asyncExecutor")
    @Transactional
    public void updateDistributionJob(Long batchId, String status) {
        log.info("START - updateDistributionJob: batchId = {}, status = {}", batchId, status);
        String jobType = gradBatchHistoryService.getJobTypeFromBatchJobHistory(batchId);
        int failedCount = 0;

        if (StringUtils.equalsIgnoreCase(status, "success")) {
            List<StudentCredentialDistribution> cList = getStudentCredentialDistributions(batchId);

            // update graduation_student_record & student_certificate
            Map<String, ServiceException> unprocessed = updateBackStudentRecords(cList, batchId, getActivitCode(jobType));
            if (!unprocessed.isEmpty()) {
                failedCount = unprocessed.size();
                status = BatchStatusEnum.FAILED.name();
                this.handleUnprocessedErrors(unprocessed);
            } else {
                status = BatchStatusEnum.COMPLETED.name();
            }
        } else {
            status = BatchStatusEnum.FAILED.name();
        }

        log.debug("updateBackStudentRecords are completed");
        // update status for batch job history
        updateDistributionBatchJobStatus(batchId, failedCount, status, populateJobParametersDTO(jobType, null));
        log.info("END - updateDistributionJob: batchId = {}, status = {}", batchId, status);
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

    private Map<String, ServiceException> updateBackStudentRecords(List<StudentCredentialDistribution> cList, Long batchId, String activityCode) {
        Map<String, ServiceException> unprocessedStudents = new HashMap<>();
        cList.forEach(scd-> {
            try {
                final String token = restUtils.getTokenResponseObject().getAccess_token();
                log.debug("Dist Job [{}] / [{}] - update student credential record & student grad record: studentID [{}]", batchId, activityCode, scd.getStudentID());
                restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,token);
                restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,token);
            } catch (Exception e) {
                unprocessedStudents.put(scd.getStudentID().toString(), new ServiceException(e));
            }
        });
        return unprocessedStudents;
    }

    private String getActivitCode(String jobType) {
        String activityCode = "MONTHLYDIST";
        if(StringUtils.isNotBlank(jobType)) {
            switch (jobType) {
                case "DISTRUN" -> activityCode = "MONTHLYDIST";
                case "DISTRUN_YE" -> activityCode = "YEARENDDIST";
                case "DISTRUN_SUPP" -> activityCode = "SUPPDIST";
                case "NONGRADRUN" -> activityCode = "NONGRADDIST";
            }
        }
        return activityCode;
    }

    private void handleUnprocessedErrors(Map<String, ServiceException> unprocessed) {
        unprocessed.forEach((k, v) -> log.error("Student with id: {} did not have distribution date updated during monthly run due to: {}", k, v.getLocalizedMessage()));
    }

    private String populateJobParametersDTO(String jobType, String credentialType) {
        JobParametersForDistribution jobParamsDto = new JobParametersForDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        String jobParamsDtoStr = null;
        try {
            jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
        } catch (Exception e) {
            log.error("Job Parameters DTO parse error for User Request Distribution - {}", e.getMessage());
        }

        return jobParamsDtoStr;
    }
}
