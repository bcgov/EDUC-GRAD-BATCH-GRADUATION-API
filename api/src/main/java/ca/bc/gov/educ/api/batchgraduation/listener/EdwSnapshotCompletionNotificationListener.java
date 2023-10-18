package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.EdwGraduationSnapshot;
import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class EdwSnapshotCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdwSnapshotCompletionNotificationListener.class);

	@Autowired
	private GradBatchHistoryService gradBatchHistoryService;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			LOGGER.info("=======================================================================================");
			LOGGER.info("EDW Snapshot Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());
			EdwSnapshotSummaryDTO summaryDTO = (EdwSnapshotSummaryDTO)jobContext.get("edwSnapshotSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new EdwSnapshotSummaryDTO();
			}
			// display Summary Details
			LOGGER.info("Schools  read     : {}", summaryDTO.getReadCount());
			LOGGER.info("Students processed: {}", summaryDTO.getProcessedCount());
			for (String school : summaryDTO.getCountMap().keySet()) {
				LOGGER.info(" school: {} => students: {}", school, summaryDTO.getCountMap().get(school));
			}
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
			if (!summaryDTO.getErrors().isEmpty()) {
				LOGGER.info(" --------------------------------------------------------------------------------------");
				EdwSnapshotSummaryDTO finalSummaryDTO = summaryDTO;
				summaryDTO.getErrors().forEach((key, value) -> LOGGER.info("  studentID [{}] - reason: {}", key, finalSummaryDTO.getErrors().get(key).getReason()));
			}
			// save batch job & error history
			saveBatchJobHistory(summaryDTO, jobExecutionId, status, endTime);
			LOGGER.info("=======================================================================================");
		}
    }

	private void saveBatchJobHistory(AlgorithmSummaryDTO summaryDTO, Long jobExecutionId, String status, Date endTime) {
		BatchGradAlgorithmJobHistoryEntity ent = gradBatchHistoryService.getGradAlgorithmJobHistory(jobExecutionId);
		if (ent != null) {
			ent.setActualStudentsProcessed(summaryDTO.getProcessedCount());
			ent.setFailedStudentsProcessed((int) summaryDTO.getErroredCount());
			ent.setEndTime(DateUtils.toLocalDateTime(endTime));
			ent.setStatus(status);

			gradBatchHistoryService.saveGradAlgorithmJobHistory(ent);
		}
	}
}
