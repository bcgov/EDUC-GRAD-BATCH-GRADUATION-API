package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "BATCH_JOB_EXECUTION")
@EqualsAndHashCode(callSuper=false)
public class BatchJobExecutionEntity {

	@Id
	@Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

	@Column(name = "JOB_INSTANCE_ID", nullable = false)
	private Long id;

	@Column(name = "CREATE_TIME", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime createTime = LocalDateTime.now();
	
	@Column(name = "START_TIME")
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime startTime;
	
	@Column(name = "END_TIME")
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime endTime;
	
	@Column(name = "STATUS", length = 10)
    private String status;

	@Column(name = "VERSION")
	private Long version;

	@Column(name = "EXIT_CODE", length = 2500)
	private String exitCode;

	@Column(name = "EXIT_MESSAGE", length = 2500)
	private String exitMessage;

	@Column(name = "LAST_UPDATED")
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime lastUpdated;

}