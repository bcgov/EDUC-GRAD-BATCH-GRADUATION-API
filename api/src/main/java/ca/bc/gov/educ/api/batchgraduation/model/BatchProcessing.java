package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchProcessing extends BaseModel{

	private UUID id;
	private String jobType;
	private String cronExpression;
	private String scheduleOccurrence;
	private String enabled;
}
