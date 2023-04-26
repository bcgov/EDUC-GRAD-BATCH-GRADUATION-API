package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ProcessError;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseWriter implements ItemWriter<GraduationStudentRecord> {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Value("#{stepExecution.jobExecution.id}")
    Long batchId;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    protected void saveBatchStatus(GraduationStudentRecord item) {
        if (summaryDTO.getErrors().containsKey(item.getStudentID())) {
            ProcessError v = summaryDTO.getErrors().get(item.getStudentID());
            gradBatchHistoryService.updateBatchStatusForStudent(batchId, item.getStudentID(), BatchStatusEnum.FAILED, v.getReason() + "-" + v.getDetail());
            summaryDTO.getErrors().remove(item.getStudentID());
        } else {
            gradBatchHistoryService.updateBatchStatusForStudent(batchId, item.getStudentID(), BatchStatusEnum.COMPLETED, null);
        }
    }
}
