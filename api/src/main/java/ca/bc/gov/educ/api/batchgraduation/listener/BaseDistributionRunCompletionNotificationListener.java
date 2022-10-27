package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public abstract class BaseDistributionRunCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDistributionRunCompletionNotificationListener.class);

    @Autowired
    private GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    RestUtils restUtils;

    protected void processBatchJobHistory(BaseDistributionSummaryDTO summaryDTO, Long jobExecutionId, String status, String jobTrigger, String jobType, Date startTime, Date endTime) {
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

        gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);

        List<BatchGradAlgorithmErrorHistoryEntity> eList = new ArrayList<>();
        summaryDTO.getErrors().forEach(e -> {
            LOGGER.info(" Student ID : {}, Reason: {}, Detail: {}", e.getStudentID(), e.getReason(), e.getDetail());
            BatchGradAlgorithmErrorHistoryEntity errorHistory = new BatchGradAlgorithmErrorHistoryEntity();
            errorHistory.setStudentID(UUID.fromString(e.getStudentID()));
            errorHistory.setJobExecutionId(jobExecutionId);
            errorHistory.setError(e.getReason() + "-" + e.getDetail());
            eList.add(errorHistory);
        });
        if(!eList.isEmpty()) {
            gradBatchHistoryService.saveGradAlgorithmErrorHistories(eList);
        }
    }

    public void schoolDistributionPrintFile(List<StudentCredentialDistribution> studentList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
        if(!studentList.isEmpty()) {
            SchoolDistributionRequest tpReq = new SchoolDistributionRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(studentList.size());
            tpReq.setStudentList(studentList);
            if(mapDist.get(usl) != null) {
                DistributionPrintRequest dist = mapDist.get(usl);
                dist.setSchoolDistributionRequest(tpReq);
                mapDist.put(usl,dist);
            }else{
                DistributionPrintRequest dist = new DistributionPrintRequest();
                dist.setSchoolDistributionRequest(tpReq);
                mapDist.put(usl,dist);
            }
        }
    }

}
