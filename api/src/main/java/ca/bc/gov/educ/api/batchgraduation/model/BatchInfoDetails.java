package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.Date;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchInfoDetails extends BaseModel{

	private Integer jobExecutionId; 
	private Date startTime;
	private Date endTime;
	private String expectedStudentsProcessed;
	private String requiredLevel;
	private String status;
}