package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.JobParametersForDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DistributionRunStatusUpdateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunStatusUpdateProcessor.class);

    private final DistributionService distributionService;

    private final RestUtils restUtils;

    @Autowired
    public DistributionRunStatusUpdateProcessor(DistributionService distributionService, RestUtils restUtils) {
        this.distributionService = distributionService;
        this.restUtils = restUtils;
    }

    @Async("asyncExecutor")
    public void process(Long batchId, String status) {
        LOGGER.info("START - DistributionRunStatusUpdateProcessor for batchId = {}, status = {}", batchId, status);
        String jobType = distributionService.getJobTypeFromBatchJobHistory(batchId);
        int failedCount = 0;

        if (StringUtils.equalsIgnoreCase(status, "success")) {
            List<StudentCredentialDistribution> cList = distributionService.getStudentCredentialDistributions(batchId);

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

        LOGGER.debug("updateBackStudentRecords are completed");
        // update status for batch job history
        distributionService.updateDistributionBatchJobStatus(batchId, failedCount, status, populateJobParametersDTO(jobType, null));
        LOGGER.info("END - DistributionRunStatusUpdateProcessor for batchId = {}, status = {}", batchId, status);
    }


    private Map<String, ServiceException> updateBackStudentRecords(List<StudentCredentialDistribution> cList, Long batchId, String activityCode) {
        Map<String, ServiceException> unprocessedStudents = new HashMap<>();
        final int totalCount = cList.size();
        final int[] processedCount = {0};
        cList.forEach(scd-> {
            try {
                final String token = restUtils.getAccessToken();
                restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,token);
                restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,token);
                processedCount[0]++;
                LOGGER.debug("Dist Job [{}] / [{}] - update {} of {} student credential record & student grad record: studentID, credentials, document status [{}, {}, {}]", batchId, activityCode, processedCount[0], totalCount, scd.getStudentID(), scd.getCredentialTypeCode(), scd.getDocumentStatusCode());

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
        unprocessed.forEach((k, v) -> LOGGER.error("Student with id: {} did not have distribution date updated during monthly run due to: {}", k, v.getLocalizedMessage()));
    }

    private String populateJobParametersDTO(String jobType, String credentialType) {
        JobParametersForDistribution jobParamsDto = new JobParametersForDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        String jobParamsDtoStr = null;
        try {
            jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
        } catch (Exception e) {
            LOGGER.error("Job Parameters DTO parse error for User Request Distribution - {}", e.getMessage());
        }

        return jobParamsDtoStr;
    }

}
