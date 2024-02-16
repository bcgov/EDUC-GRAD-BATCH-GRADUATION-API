package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class UserScheduledCompletionNotificationListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserScheduledCompletionNotificationListener.class);
    private static final String LOG_SEPARATION = "=======================================================================================";

//	@Autowired
	private GradBatchHistoryService gradBatchHistoryService;
//	@Autowired
	private TaskSchedulingService taskSchedulingService;

//	@Autowired
	private RestUtils restUtils;

	public UserScheduledCompletionNotificationListener(GradBatchHistoryService gradBatchHistoryService,
													   TaskSchedulingService taskSchedulingService,
													   RestUtils restUtils) {
		this.gradBatchHistoryService = gradBatchHistoryService;
		this.taskSchedulingService = taskSchedulingService;
		this.restUtils = restUtils;
	}
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			LOGGER.info(LOG_SEPARATION);
	    	LOGGER.info("User Scheduled Jobs Refresher completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			LOGGER.info(LOG_SEPARATION);
		}
    }
}
