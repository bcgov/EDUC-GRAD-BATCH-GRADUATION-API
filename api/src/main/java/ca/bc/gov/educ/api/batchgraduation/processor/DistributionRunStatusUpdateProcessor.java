package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DistributionRunStatusUpdateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunStatusUpdateProcessor.class);

    private final DistributionService distributionService;

    private final RestUtils restUtils;

    private final JsonTransformer jsonTransformer;

    @Autowired
    public DistributionRunStatusUpdateProcessor(DistributionService distributionService, RestUtils restUtils, JsonTransformer jsonTransformer) {
        this.distributionService = distributionService;
        this.restUtils = restUtils;
        this.jsonTransformer = jsonTransformer;
    }

    @Async("asyncExecutor")
    public void process(Long batchId, String status) {
        LOGGER.info("START - DistributionRunStatusUpdateProcessor for batchId = {}, status = {}", batchId, status);
        String jobType = distributionService.getJobTypeFromBatchJobHistory(batchId);
        int failedCount = 0;

        if (StringUtils.equalsIgnoreCase(status, "success")) {
            List<StudentCredentialDistribution> cList = distributionService.getStudentCredentialDistributions(batchId);

            // update graduation_student_record & student_certificate
            Map<String, ServiceException> unprocessed = updateBackStudentRecords(cList, batchId, jobType);
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
        distributionService.updateDistributionBatchJobStatus(batchId, failedCount, status);
        LOGGER.info("END - DistributionRunStatusUpdateProcessor for batchId = {}, status = {}", batchId, status);
    }


    private Map<String, ServiceException> updateBackStudentRecords(List<StudentCredentialDistribution> cList, Long batchId, String jobType) {
        Map<String, ServiceException> unprocessedStudents = new HashMap<>();
        String activityCode = getActivitCode(jobType);
        final int totalCredentialCount = cList.size();
        List<UUID> studentIDs = cList.stream().map(StudentCredentialDistribution::getStudentID).distinct().toList();
        final int totalStudentCount = studentIDs.size();

        final int[] processedCount = {1};
        // Credential Records
        cList.forEach(scd-> {
            try {
                final String accessToken = restUtils.getAccessToken();
                restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),
                        "NONGRADYERUN".equalsIgnoreCase(activityCode)? "IP" : scd.getDocumentStatusCode(),activityCode,accessToken);
                LOGGER.debug("Dist Job [{}] / [{}] - update {} of {} student credential record: studentID, credentials, document status [{}, {}, {}]", batchId, activityCode, processedCount[0], totalCredentialCount, scd.getStudentID(), scd.getCredentialTypeCode(), scd.getDocumentStatusCode());
                processedCount[0]++;
            } catch (Exception e) {
                unprocessedStudents.put(scd.getStudentID().toString(), new ServiceException(e));
                LOGGER.error("Unexpected Error on update {} of {} student credential record: studentID [{}] \n {}",
                        processedCount[0], totalCredentialCount, scd.getStudentID(), e.getMessage());
            }
        });

        processedCount[0] = 1;
        // Unique Students
        studentIDs.forEach(uuid-> {
            try {
                if(!StringUtils.equalsAnyIgnoreCase(jobType, "REGALG", "TVRRUN")) {
                    restUtils.updateStudentGradRecord(uuid, batchId, activityCode);
                    LOGGER.debug("Dist Job [{}] / [{}] - update {} of {} student grad record: studentID [{}]", batchId, activityCode, processedCount[0], totalStudentCount, uuid);
                }
                processedCount[0]++;
            } catch (Exception e) {
                unprocessedStudents.put(uuid.toString(), new ServiceException(e));
                LOGGER.error("Unexpected Error on create {} of {} audit history: studentID [{}] \n {}",
                        processedCount[0], totalStudentCount, uuid, e.getMessage());
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
                case "NONGRADRUN" -> activityCode = "NONGRADYERUN";
            }
        }
        return activityCode;
    }

    private void handleUnprocessedErrors(Map<String, ServiceException> unprocessed) {
        unprocessed.forEach((k, v) -> LOGGER.error("Student with id: {} did not have distribution date updated during monthly run due to: {}", k, v.getLocalizedMessage()));
    }

}
