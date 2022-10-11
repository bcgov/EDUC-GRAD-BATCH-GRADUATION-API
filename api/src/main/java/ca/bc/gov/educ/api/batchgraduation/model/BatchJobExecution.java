package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchJobExecution {

	private Long jobExecutionId;
	private Date createTime;
	private Date startTime;
	private Date endTime;
	private String status;
}
