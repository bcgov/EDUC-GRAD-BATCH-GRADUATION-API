package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import com.google.common.collect.Lists;
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
        final int partitionSize = 999;

        final int[] processedCount = {1};
        // Credential Records
        if(!cList.isEmpty()) {
            List<List<StudentCredentialDistribution>> studentCredPartitions = Lists.partition(cList, partitionSize);
            studentCredPartitions.forEach(studentCredPartition -> {
                if ("NONGRADYERUN".equalsIgnoreCase(activityCode)) { studentCredPartition.forEach(entry -> entry.setDocumentStatusCode("IP")); }
                final String accessToken = restUtils.getAccessToken();
                Integer processed = restUtils.updateStudentCredentialRecords(studentCredPartition, activityCode, accessToken);
                if(studentCredPartition.size() != processed) {
                    studentCredPartition.stream().forEach(scd -> unprocessedStudents.put(scd.getStudentID().toString(), new ServiceException("Updating student credential failed.")));
                }
                processedCount[0] = processedCount[0] + processed;
            });
            LOGGER.debug("Dist Job [{}] / [{}] - updated {} of {} student credential record", batchId, activityCode, processedCount[0], totalCredentialCount);
            if (cList.size() != processedCount[0]) {
                LOGGER.error("Dist Job [{}] / [{}] - Unexpected Error on updating student credential record {} of {} ",
                        batchId, activityCode, (totalCredentialCount - processedCount[0]), totalCredentialCount);
            }
        }

        // Unique Students
        processedCount[0] = 1;
        if(!StringUtils.equalsAnyIgnoreCase(jobType, "REGALG", "TVRRUN") && !studentIDs.isEmpty()) {
            List<List<UUID>> studentIDPartitions = Lists.partition(studentIDs, partitionSize);
            studentIDPartitions.forEach(studentIDPartition -> {
                processedCount[0]  = processedCount[0] + restUtils.updateStudentGradRecordHistory(studentIDPartition, batchId, ThreadLocalStateUtil.getCurrentUser(), activityCode);
            });
            LOGGER.debug("Dist Job [{}] / [{}] - updated {} of {} student grad record & history", batchId, activityCode, processedCount[0], totalStudentCount);
            if(studentIDs.size() != processedCount[0]) {
                LOGGER.error("Dist Job [{}] / [{}] - Unexpected Error on updating {} of {} student grad record & history",
                        batchId, activityCode, (totalStudentCount - processedCount[0]), totalStudentCount);
            }
        }
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
