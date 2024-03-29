package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.service.TaskDefinition;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class UserScheduledProcessor implements ItemProcessor<UserScheduledJobs, UserScheduledJobs> {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserScheduledProcessor.class);

	@Autowired TaskDefinition taskDefinition;
	@Autowired TaskSchedulingService taskSchedulingService;
	@Autowired JsonTransformer jsonTransformer;
    
	@Override
	public UserScheduledJobs process(UserScheduledJobs item) throws Exception {
		LOGGER.info("Processing = {}", item.getId());
		Task task = (Task)jsonTransformer.unmarshall(item.getJobParameters(), Task.class);
		task.setJobIdReference(item.getId());
		taskDefinition.setTask(task);
		taskSchedulingService.scheduleATask(item.getId(),taskDefinition, task.getCronExpression());
		return item;
	}

    
}
