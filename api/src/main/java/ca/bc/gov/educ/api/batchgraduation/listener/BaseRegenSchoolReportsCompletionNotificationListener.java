package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import ca.bc.gov.educ.api.batchgraduation.util.GradSorter;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.USER_SCHEDULED;

public abstract class BaseRegenSchoolReportsCompletionNotificationListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseRegenSchoolReportsCompletionNotificationListener.class);

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    DistributionService distributionService;

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Autowired
    RestUtils restUtils;

    protected void processBatchJobHistory(SchoolReportsRegenSummaryDTO summaryDTO, Long jobExecutionId, String status, String jobTrigger, String jobType, Date startTime, Date endTime, String jobParameters) {
        int failedRecords = summaryDTO.getErrors().size();
        Long processedStudents = summaryDTO.getProcessedCount();
        Long expectedStudents = summaryDTO.getReadCount();

        BatchGradAlgorithmJobHistoryEntity ent = new BatchGradAlgorithmJobHistoryEntity();
        ent.setActualStudentsProcessed(processedStudents);
        ent.setExpectedStudentsProcessed(expectedStudents);
        ent.setFailedStudentsProcessed(failedRecords);
        ent.setJobExecutionId(jobExecutionId);
        ent.setStartTime(DateUtils.toLocalDateTime(startTime));
        ent.setEndTime(DateUtils.toLocalDateTime(endTime));
        ent.setStatus(status);
        ent.setTriggerBy(jobTrigger);
        ent.setJobType(jobType);
        ent.setJobParameters(jobParameters);

        gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
    }

    protected String buildJobParametersDTO(String jobType, String studentSearchRequest, TaskSelection taskSelection, String taskSelectionOptionType) {
        String jobParamsDtoStr = null;

        if (taskSelection == null) {
            // Distribution (Monthly, Year-end, Year-end NonGrad, Supplemental)
            jobParamsDtoStr = populateJobParametersDTO(jobType, null, studentSearchRequest);
        } else {
            jobParamsDtoStr = switch (taskSelection) {
                case SRRBJ -> // School Reports Regen Batch Job
                        populateJobParametersDTO(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
                default -> jobParamsDtoStr;
            };
        }

        return jobParamsDtoStr != null? jobParamsDtoStr : studentSearchRequest;
    }

    private String populateJobParametersDTO(String jobType, String credentialType, String studentSearchRequest) {
        JobParametersForDistribution jobParamsDto = new JobParametersForDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        if (StringUtils.isNotBlank(studentSearchRequest)) {
            StudentSearchRequest payload = (StudentSearchRequest)jsonTransformer.unmarshall(studentSearchRequest, StudentSearchRequest.class);
            jobParamsDto.setPayload(payload);
        }

        return jsonTransformer.marshall(jobParamsDto);
    }

    protected StudentSearchRequest getStudentSearchRequest(String searchRequest) {
        if(StringUtils.isNotBlank(searchRequest)) {
            return (StudentSearchRequest) jsonTransformer.unmarshall(searchRequest, StudentSearchRequest.class);
        }
        return new StudentSearchRequest();
    }

    long getElapsedTimeMillis(JobExecution jobExecution) {
        return new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    void updateUserSchedulingJobs(JobParameters jobParameters) {
        String userScheduledId = jobParameters.getString(USER_SCHEDULED);
        if (userScheduledId != null) {
            taskSchedulingService.updateUserScheduledJobs(userScheduledId);
        }
    }
}
