package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchGradAlgorithmJobHistory extends BaseModel{

	private UUID id; 
	private Integer jobExecutionId; 
	private Date startTime;
	private Date endTime;
	private String expectedStudentsProcessed;
	private String actualStudentsProcessed;
	private String failedStudentsProcessed;
	private String status;
	private String triggerBy;
}
