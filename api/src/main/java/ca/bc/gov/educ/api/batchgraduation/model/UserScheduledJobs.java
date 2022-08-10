package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class UserScheduledJobs extends BaseModel{

	private UUID id;
	private String jobCode;
	private String cronExpression;
	private String jobName;
	private String jobParameters;
	private String status;
}
