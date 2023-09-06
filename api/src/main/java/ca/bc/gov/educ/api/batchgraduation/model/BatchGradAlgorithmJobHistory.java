package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchGradAlgorithmJobHistory extends BaseModel{

	private UUID id; 
	private Integer jobExecutionId; 
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String expectedStudentsProcessed;
	private String actualStudentsProcessed;
	private String failedStudentsProcessed;
	private String status;
	private String triggerBy;
	private String jobType;
	private String localDownload;
	private String jobParameters;
}
