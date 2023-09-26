package ca.bc.gov.educ.api.batchgraduation.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class ArchiveStudentsRunCompletionNotificationListener extends BaseRunCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsRunCompletionNotificationListener.class);
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			LOGGER.info("=======================================================================================");
	    	LOGGER.info("Archive Students Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			handleSummary(jobExecution, "ArchiveStudentsJobSummaryDTO", false);
			LOGGER.info("=======================================================================================");
		}
    }
}
