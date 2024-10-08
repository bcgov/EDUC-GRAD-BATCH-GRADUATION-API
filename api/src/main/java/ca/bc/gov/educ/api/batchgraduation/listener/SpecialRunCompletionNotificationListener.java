package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@Component
public class SpecialRunCompletionNotificationListener extends BaseRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialRunCompletionNotificationListener.class);

	@Autowired
	RestUtils restUtils;

	@Autowired
	JsonTransformer jsonTransformer;

	private static final String RUN_BY = "runBy";
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Special Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			handleSummary(jobExecution, "spcRunAlgSummaryDTO", true);
			processGradStudentRecordJobHistory(jobExecution);
			LOGGER.info("=======================================================================================");
		}
    }

	private void processGradStudentRecordJobHistory(JobExecution jobExecution) {

		JobParameters jobParameters = jobExecution.getJobParameters();
		Long batchId = jobExecution.getId();
		String userName = jobParameters.getString(RUN_BY);

		String searchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
		StudentSearchRequest req = (StudentSearchRequest)jsonTransformer.unmarshall(searchRequest, StudentSearchRequest.class);

		restUtils.updateStudentGradRecordHistory(List.of(), batchId, userName, StringUtils.upperCase(req.getActivityCode()));
	}


}
