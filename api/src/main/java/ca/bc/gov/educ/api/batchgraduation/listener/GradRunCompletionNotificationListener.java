package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.service.TvrLaunchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.JOB_TRIGGER;

@Slf4j
@Component
public class GradRunCompletionNotificationListener extends BaseRunCompletionNotificationListener {

	private static final String BATCH = "BATCH";
	private final TvrLaunchService tvrLaunchService;

	@Autowired
	public GradRunCompletionNotificationListener(TvrLaunchService tvrLaunchService) {
		this.tvrLaunchService = tvrLaunchService;
    }

	@Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			log.info("=======================================================================================");
	    	log.info("Grad Algorithm Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus());
			handleSummary(jobExecution, "regGradAlgSummaryDTO", false);
			log.info("========================================================================================");
			String jobTrigger = jobExecution.getJobParameters().getString(JOB_TRIGGER);
			if (BATCH.equalsIgnoreCase(jobTrigger)) {
				log.info("Launching TVR batch...");
				tvrLaunchService.launchTVRReportProcess();
			}
		}
    }
}
