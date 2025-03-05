package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@Component
public class DeleteStudentReportsCompletionNotificationListener extends BaseDistributionRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteStudentReportsCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedTimeMillis = getElapsedTimeMillis(jobExecution);
			LOGGER.info("=======================================================================================");
			JobParameters jobParameters = jobExecution.getJobParameters();
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			Long jobExecutionId = jobExecution.getId();
			String jobType = jobParameters.getString("jobType");
			LOGGER.info("{} Delete Student Reports Job {} completed in {} s with jobExecution status {}", jobType, jobExecutionId, elapsedTimeMillis / 1000, jobExecution.getStatus());

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

			updateUserSchedulingJobs(jobParameters);

			StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
			String userName = extractUserName(summaryDTO, jobParameters, searchRequest);

			summaryDTO.getSchools().forEach((value) -> LOGGER.info("School {} number of Deleted Student Reports : {}", value.getSchoolId(), value.getNumberOfStudents()));
			if(summaryDTO.getProcessedCount() > 0) {
				List<UUID> finalStudentGuids = searchRequest.getStudentIDs();
				int partitionSize = finalStudentGuids.size()/200;
				partitionSize = partitionSize == 0 ? finalStudentGuids.size() : partitionSize;
				for (int i = 0; i < finalStudentGuids.size(); i += partitionSize) {
					List<UUID> studentGuidsSubList = finalStudentGuids.subList(i, Math.min(i + partitionSize, finalStudentGuids.size()));
					restUtils.updateStudentGradRecordHistory(studentGuidsSubList, jobExecutionId, userName, "TVRDELETED");
				}
			}
			String jobParametersDTO = buildJobParametersDTO(jobType, studentSearchRequest, null, null);
			// save batch job & error history
			processBatchJobHistory(summaryDTO, jobExecutionId, status, jobTrigger, jobType, startTime, endTime, jobParametersDTO);
			LOGGER.info(" --------------------------------------------------------------------------------------");

		}
	}
}
