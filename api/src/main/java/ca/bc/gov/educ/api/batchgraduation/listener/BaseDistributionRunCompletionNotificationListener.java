package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.util.GradSorter;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

public abstract class BaseDistributionRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDistributionRunCompletionNotificationListener.class);

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    DistributionService distributionService;

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
            // Distribution (Monthly, Year-end, Year-end NonGrad, Supplemental)
            jobParamsDtoStr = populateJobParametersDTO(jobType, null, studentSearchRequest);
        } else {
            jobParamsDtoStr = switch (taskSelection) {
                case URDBJ -> // User Request Distribution
                        populateJobParametersDTO(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
                case BDBJ -> // Blank Distribution
                        populateJobParametersDTOForBlankDistribution(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
                case URPDBJ -> // PSI Distribution
                        populateJobParametersDTOForPsiDistribution(taskSelection.getValue(), taskSelectionOptionType, studentSearchRequest);
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

    private String populateJobParametersDTOForBlankDistribution(String jobType, String credentialType, String studentSearchRequest) {
        JobParametersForBlankDistribution jobParamsDto = new JobParametersForBlankDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        if(StringUtils.isNotBlank(studentSearchRequest)) {
            BlankCredentialRequest payload = (BlankCredentialRequest) jsonTransformer.unmarshall(studentSearchRequest, BlankCredentialRequest.class);
            jobParamsDto.setPayload(payload);
        }
        return jsonTransformer.marshall(jobParamsDto);
    }

    private String populateJobParametersDTOForPsiDistribution(String jobType, String transmissionType, String studentSearchRequest) {
        JobParametersForPsiDistribution jobParamsDto = new JobParametersForPsiDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setTransmissionType(transmissionType);

        if(StringUtils.isNotBlank(studentSearchRequest)) {
            PsiCredentialRequest payload = (PsiCredentialRequest) jsonTransformer.unmarshall(studentSearchRequest, PsiCredentialRequest.class);
            jobParamsDto.setPayload(payload);
        }

        String jobParamsDtoStr = jsonTransformer.marshall(jobParamsDto);
        return jobParamsDtoStr != null? jobParamsDtoStr : studentSearchRequest;
    }

    protected StudentSearchRequest getStudentSearchRequest(String searchRequest) {
        if(StringUtils.isNotBlank(searchRequest)) {
            return (StudentSearchRequest) jsonTransformer.unmarshall(searchRequest, StudentSearchRequest.class);
        }
        return new StudentSearchRequest();
    }

    void sortStudentCredentialDistribution(List<StudentCredentialDistribution> students) {
        GradSorter.sortStudentCredentialDistributionByNames(students);
    }

    void filterStudentCredentialDistribution(List<StudentCredentialDistribution> credentialList, String activityCode) {
        LOGGER.debug("Filter {} Student Credential Distribution for {} student credentials", activityCode, credentialList.size());
        if("NONGRADYERUN".equalsIgnoreCase(activityCode)) {
            LOGGER.debug("Apply {} filters for the list of {} students", "NONGRADYERUN", credentialList.size());
            credentialList.removeIf(s->"SCCP".equalsIgnoreCase(s.getProgram()));
            credentialList.removeIf(s->"1950".equalsIgnoreCase(s.getProgram()) && !"AD".equalsIgnoreCase(s.getStudentGrade()));
            credentialList.removeIf(s->!"1950".equalsIgnoreCase(s.getProgram()) && !"12".equalsIgnoreCase(s.getStudentGrade()));
        }
        LOGGER.debug("Total {} selected after filter", credentialList.size());
    }
}
