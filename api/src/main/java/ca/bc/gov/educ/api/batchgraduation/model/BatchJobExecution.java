package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchJobExecution {

	private Long jobExecutionId;
	private LocalDateTime createTime;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String status;
}
