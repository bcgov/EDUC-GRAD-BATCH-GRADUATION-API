package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@Component
public class ArchiveStudentsCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
			LOGGER.info("=======================================================================================");
			JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String jobType = jobParameters.getString("jobType");
			LOGGER.info("{} Archive Students Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis / 1000, jobExecution.getStatus());

			String status = jobExecution.getStatus().toString();
			Date startTime = DateUtils.toDate(jobExecution.getStartTime());
			Date endTime = DateUtils.toDate(jobExecution.getEndTime());
			String jobTrigger = jobParameters.getString("jobTrigger");

			DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get("distributionSummaryDTO");

			String studentSearchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
			// display Summary Details
			LOGGER.info("Records read   : {}", summaryDTO.getReadCount());
			LOGGER.info("Processed count: {}", summaryDTO.getProcessedCount());
			LOGGER.info(" --------------------------------------------------------------------------------------");
			LOGGER.info("Errors:{}", summaryDTO.getErrors().size());

			StudentSearchRequest payload = (StudentSearchRequest)jsonTransformer.unmarshall(studentSearchRequest, StudentSearchRequest.class);
			String userName = ObjectUtils.defaultIfNull(payload.getUser(), "Batch Student Archive Process");

			updateUserSchedulingJobs(jobParameters);

			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, null, null);
			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(" --------------------------------------------------------------------------------------");
			summaryDTO.getSchools().forEach((value) -> LOGGER.info("School {} number of archived Students : {}", value.getMincode(), value.getNumberOfStudents()));
			restUtils.updateStudentGradRecordHistory(jobExecutionId, userName, "USERSTUDARC");
		}
	}
}
