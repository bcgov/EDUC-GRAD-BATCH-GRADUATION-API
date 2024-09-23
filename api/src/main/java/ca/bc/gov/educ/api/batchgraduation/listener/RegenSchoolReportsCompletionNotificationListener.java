package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BaseSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportsRegenSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class RegenSchoolReportsCompletionNotificationListener extends BaseRegenSchoolReportsCompletionNotificationListener {

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
			log.info("=======================================================================================");
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			log.info("Regen School Reports Job {} completed in {} s with jobExecution status {}", jobExecutionId, elapsedTimeMillis / 1000, jobExecution.getStatus());

			String status = jobExecution.getStatus().toString();
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());

			SchoolReportsRegenSummaryDTO summaryDTO = (SchoolReportsRegenSummaryDTO) jobContext.get("schoolReportsRegenSummaryDTO");

			// display Summary Details
            assert summaryDTO != null;
            log.info("Records read   : {}", summaryDTO.getReadCount());
			log.info("Processed count: {}", summaryDTO.getProcessedCount());
			log.info(" --------------------------------------------------------------------------------------");
			log.info("Errors:{}", summaryDTO.getErrors().size());
			log.info(" --------------------------------------------------------------------------------------");
			summaryDTO.getSchools().forEach(value -> log.debug("School Report regenerated for {}", value.getMincode()));
			// save batch job & error history
			saveBatchJobHistory(summaryDTO, jobExecutionId, status, endTime);

		}
	}

	private void saveBatchJobHistory(BaseSummaryDTO summaryDTO, Long jobExecutionId, String status, Date endTime) {
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
