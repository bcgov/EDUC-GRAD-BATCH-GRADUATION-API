package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public abstract class BaseDistributionRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDistributionRunCompletionNotificationListener.class);

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    RestUtils restUtils;

    protected void processBatchJobHistory(BaseDistributionSummaryDTO summaryDTO, Long jobExecutionId, String status, String jobTrigger, String jobType, Date startTime, Date endTime, String jobParameters) {
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(jobExecutionId);
        ent.setStartTime(startTime);
        ent.setEndTime(endTime);
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);
        ent.setJobParameters(jobParameters);

        gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
    }

    protected String buildJobParametersDTO(String jobType, String studentSearchRequest, TaskSelection taskSelection, String taskSelectionOptionType) {
        String jobParamsDtoStr = null;

        if (taskSelection == null) {
            // Scheduled Distribution (Monthly or Yearly)
            jobParamsDtoStr = populateJobParametersDTO(jobType, null, studentSearchRequest);
        } else {
            switch (taskSelection) {
                case URDBJ: // User Request Distribution
                    jobParamsDtoStr = populateJobParametersDTO(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
                    break;
                case BDBJ: // Blank Distribution
                    jobParamsDtoStr = populateJobParametersDTOForBlankDistribution(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
                    break;
                case URPDBJ: // PSI Distribution
                    jobParamsDtoStr = populateJobParametersDTOForPsiDistribution(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
                    break;
            }
        }

        return jobParamsDtoStr != null? jobParamsDtoStr : studentSearchRequest;
    }

    private String populateJobParametersDTO(String jobType, String credentialType, String studentSearchRequest) {
        JobParametersForDistribution jobParamsDto = new JobParametersForDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        if (StringUtils.isNotBlank(studentSearchRequest)) {
            try {
                StudentSearchRequest payload = new ObjectMapper().readValue(studentSearchRequest, StudentSearchRequest.class);
                jobParamsDto.setPayload(payload);
            } catch (Exception e) {
                LOGGER.error("StudentSearchRequest payload parse error - {}", e.getMessage());
            }
        }

        String jobParamsDtoStr = null;
        try {
            jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
        } catch (Exception e) {
            LOGGER.error("Job Parameters DTO parse error for User Request Distribution - {}", e.getMessage());
        }

        return jobParamsDtoStr;
    }

    private String populateJobParametersDTOForBlankDistribution(String jobType, String credentialType, String studentSearchRequest) {
        JobParametersForBlankDistribution jobParamsDto = new JobParametersForBlankDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        try {
            BlankCredentialRequest payload = new ObjectMapper().readValue(studentSearchRequest, BlankCredentialRequest.class);
            jobParamsDto.setPayload(payload);
        } catch (Exception e) {
            LOGGER.error("BlankCredentialRequest payload parse error - {}", e.getMessage());
        }

        String jobParamsDtoStr = null;
        try {
            jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
        } catch (Exception e) {
            LOGGER.error("Job Parameters DTO parse error for Blank Distribution - {}", e.getMessage());
        }

        return jobParamsDtoStr;
    }

    private String populateJobParametersDTOForPsiDistribution(String jobType, String transmissionType, String studentSearchRequest) {
        JobParametersForPsiDistribution jobParamsDto = new JobParametersForPsiDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setTransmissionType(transmissionType);

        try {
            PsiCredentialRequest payload = new ObjectMapper().readValue(studentSearchRequest, PsiCredentialRequest.class);
            jobParamsDto.setPayload(payload);
        } catch (Exception e) {
            LOGGER.error("PsiCredentialRequest payload parse error - {}", e.getMessage());
        }

        String jobParamsDtoStr = null;
        try {
            jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
        } catch (Exception e) {
            LOGGER.error("Job Parameters DTO parse error for PSI Distribution - {}", e.getMessage());
        }

        return jobParamsDtoStr != null? jobParamsDtoStr : studentSearchRequest;
    }

}
