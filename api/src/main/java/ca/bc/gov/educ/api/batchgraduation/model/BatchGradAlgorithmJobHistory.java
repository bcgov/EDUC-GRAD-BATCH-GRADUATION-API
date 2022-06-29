package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

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
	private String jobType;
	private String localDownload;
}
