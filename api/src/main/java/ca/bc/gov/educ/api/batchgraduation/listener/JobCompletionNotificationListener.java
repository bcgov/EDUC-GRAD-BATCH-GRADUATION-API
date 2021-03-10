package ca.bc.gov.educ.api.batchgraduation.listener;

import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.api.batchgraduation.util.GradDataStore;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    @Autowired
    private GradDataStore gradDataStore;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
    	if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
	    	long elapsedTimeMillis = new Date().getTime() - jobExecution.getStartTime().getTime();
	    	LOGGER.info("Job completed in {} s with jobExecution status {}", elapsedTimeMillis/1000, jobExecution.getStatus().toString());
	    	ThreadLocal<Map<String, Integer>> map = gradDataStore.getProgramMap();
	    	map.get().forEach((k,v) -> {
	    		LOGGER.info("Number of Students Processed from "+k+" progam : "+v);
	    	});
	    	LOGGER.info("Number of Students Processed : "+gradDataStore.getSizeOfProcessedItem());
	    	gradDataStore.clear();
		}
    }
}
