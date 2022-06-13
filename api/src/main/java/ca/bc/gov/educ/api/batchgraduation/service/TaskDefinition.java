package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskDefinition implements Runnable{

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDefinition.class);

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";
    private static final String SEARCH_REQUEST = "searchRequest";
    private static final String MANUAL = "MANUAL";
    private static final String CREDENTIAL_TYPE = "credentialType";
    private static final String ERROR_MSG = "Error {}";

    @Autowired JobLauncher jobLauncher;
    @Autowired JobRegistry jobRegistry;

    private Task task;

    @Override
    public void run() {
        TaskSelection taskType = TaskSelection.valueOf(StringUtils.toRootUpperCase(task.getJobName()));
        JobSelection jobType = JobSelection.valueOf(StringUtils.toRootUpperCase(task.getJobName()));
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, jobType.name());
        if(task.getCredentialType() != null) {
            builder.addString(CREDENTIAL_TYPE,task.getCredentialType());
        }
        if(task.getPayload() != null) {
            AlgorithmSummaryDTO validate = validateInput(task.getPayload());
            if (validate == null) {
                try {
                    String studentSearchData = new ObjectMapper().writeValueAsString(task.getPayload());
                    executeBatchJob(builder, taskType,studentSearchData);
                }catch (JsonProcessingException e) {
                    LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
                }
            }
        }
        if(task.getBlankPayLoad() != null) {
            BlankDistributionSummaryDTO validate = validateInputBlankDisRun(task.getBlankPayLoad());
            if (validate == null) {
                try {
                    String blankSearchData = new ObjectMapper().writeValueAsString(task.getBlankPayLoad());
                    executeBatchJob(builder, taskType,blankSearchData);
                } catch (JsonProcessingException e) {
                    LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
                }
            }
        }

    }

    private void executeBatchJob(JobParametersBuilder builder, TaskSelection taskType, String data) {
        builder.addString(SEARCH_REQUEST, data);
        try {
            jobLauncher.run(jobRegistry.getJob(taskType.name()), builder.toJobParameters());
        }catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            LOGGER.debug(ERROR_MSG, e.getLocalizedMessage());
        }
    }
    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    private AlgorithmSummaryDTO validateInput(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getPens().isEmpty() && studentSearchRequest.getDistricts().isEmpty() && studentSearchRequest.getSchoolCategoryCodes().isEmpty() && studentSearchRequest.getPrograms().isEmpty() && studentSearchRequest.getSchoolOfRecords().isEmpty()) {
            AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.setException("Please provide at least 1 parameter");
            return summaryDTO;
        }
        return null;
    }

    private BlankDistributionSummaryDTO validateInputBlankDisRun(BlankCredentialRequest blankCredentialRequest) {
        if(blankCredentialRequest.getSchoolOfRecords().isEmpty() || blankCredentialRequest.getCredentialTypeCode().isEmpty()) {
            BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
            summaryDTO.setException("Please provide both parameters");
            return summaryDTO;
        }
        return null;
    }
}
