package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.util.GradSorter;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.transform.TransformerException;
import java.util.Date;
import java.util.Iterator;
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

    protected StudentSearchRequest getStudentSearchRequest(String searchRequest) {
        StudentSearchRequest request;
        try {
            request = (StudentSearchRequest)jsonTransformer.unmarshall(searchRequest, StudentSearchRequest.class);
        } catch (TransformerException e) {
            LOGGER.warn("Unable to deserialize StudentSearchRequest object");
            request = new StudentSearchRequest();
        }
        return request;
    }

    void sortStudentCredentialDistribution(List<StudentCredentialDistribution> students) {
        GradSorter.sortStudentCredentialDistributionByNames(students);
    }

    void filterStudentCredentialDistribution(List<StudentCredentialDistribution> credentialList, String searchRequest, String activityCode) {
        LOGGER.debug("Filter Student Credential Distribution for {} student credentials", credentialList.size());
        StudentSearchRequest request = getStudentSearchRequest(searchRequest);
        Iterator scdIt = credentialList.iterator();
        while (scdIt.hasNext()) {
            StudentCredentialDistribution scd = (StudentCredentialDistribution)scdIt.next();
            String districtCode = StringUtils.substring(scd.getSchoolOfRecord(), 0, 3);
            if (
                    (request.getDistricts() != null && !request.getDistricts().isEmpty() && !request.getDistricts().contains(districtCode))
                            ||
                    (request.getSchoolOfRecords() != null && !request.getSchoolOfRecords().isEmpty() && !request.getSchoolOfRecords().contains(scd.getSchoolOfRecord()))
            ) {
                scdIt.remove();
                LOGGER.debug("Student Credential {}/{} removed by the filters \"{}\" and \"{}\"", scd.getPen(), scd.getSchoolOfRecord(), String.join(",", request.getDistricts()), String.join(",", request.getSchoolCategoryCodes()));
            }
        }
        if("NONGRADDIST".equalsIgnoreCase(activityCode)) {
            LOGGER.debug("Apply {} filters for the list of {} students", "NONGRADDIST", credentialList.size());
            credentialList.removeIf(s->"SCCP".equalsIgnoreCase(s.getProgram()));
            credentialList.removeIf(s->"1950".equalsIgnoreCase(s.getProgram()) && !"AD".equalsIgnoreCase(s.getStudentGrade()) && !StringUtils.isBlank(s.getProgramCompletionDate()));
            credentialList.removeIf(s->!"1950".equalsIgnoreCase(s.getProgram()) && !"12".equalsIgnoreCase(s.getStudentGrade()) && !StringUtils.isBlank(s.getProgramCompletionDate()));
        }
        LOGGER.debug("Total {} selected after filter", credentialList.size());
    }
}
