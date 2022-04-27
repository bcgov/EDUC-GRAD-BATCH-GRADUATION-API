package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchGradAlgorithmErrorHistory extends BaseModel{

	private UUID id; 
	private Long jobExecutionId;
	private UUID studentID;
	private String error;
}
