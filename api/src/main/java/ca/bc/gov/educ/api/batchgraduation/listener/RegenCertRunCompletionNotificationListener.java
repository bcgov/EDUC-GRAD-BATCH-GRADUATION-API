package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class RegenCertRunCompletionNotificationListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegenCertRunCompletionNotificationListener.class);

	@Autowired
	private GradBatchHistoryService gradBatchHistoryService;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			LOGGER.info("=======================================================================================");
			LOGGER.info("Regenerate Certificate Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String status = jobExecution.getStatus().toString();
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());
			AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("regenCertSummaryDTO");
			if(summaryDTO == null) {
				summaryDTO = new AlgorithmSummaryDTO();
			}
			// display Summary Details
			LOGGER.info("Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info("Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());
			if (!summaryDTO.getErrors().isEmpty()) {
				LOGGER.info(" --------------------------------------------------------------------------------------");
				AlgorithmSummaryDTO finalSummaryDTO = summaryDTO;
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
