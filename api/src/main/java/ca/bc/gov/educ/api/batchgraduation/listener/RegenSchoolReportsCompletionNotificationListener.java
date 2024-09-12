package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportsRegenSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@Slf4j
@Component
public class RegenSchoolReportsCompletionNotificationListener extends BaseRegenSchoolReportsCompletionNotificationListener {

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
			log.info("=======================================================================================");
			JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String jobType = jobParameters.getString("jobType");
			log.info("{} Regen School Reports Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis / 1000, jobExecution.getStatus());

			String status = jobExecution.getStatus().toString();
			Date startTime = DateUtils.toDate(jobExecution.getStartTime());
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());
			String jobTrigger = jobParameters.getString("jobTrigger");

			SchoolReportsRegenSummaryDTO summaryDTO = (SchoolReportsRegenSummaryDTO) jobContext.get("schoolReportsRegenSummaryDTO");

			String studentSearchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
			// display Summary Details
			log.info("Records read   : {}", summaryDTO.getReadCount());
			log.info("Processed count: {}", summaryDTO.getProcessedCount());
			log.info(" --------------------------------------------------------------------------------------");
			log.info("Errors:{}", summaryDTO.getErrors().size());

			updateUserSchedulingJobs(jobParameters);

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, null, null);
			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			log.info(" --------------------------------------------------------------------------------------");
			summaryDTO.getSchools().forEach((value) -> log.info("School {} number of Regen School Reports : {}", value.getMincode(), value.getNumberOfSchoolReports()));

		}
	}
}
