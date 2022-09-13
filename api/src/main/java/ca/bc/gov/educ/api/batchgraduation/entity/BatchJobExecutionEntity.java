package ca.bc.gov.educ.api.batchgraduation.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

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

	@Column(name = "CREATE_TIME", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date createTime;
	
	@Column(name = "START_TIME", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date startTime;
	
	@Column(name = "END_TIME", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date endTime;
	
	@Column(name = "STATUS", nullable = true)
    private String status;

	@Column(name = "VERSION")
	private Long version;

	@Column(name = "EXIT_CODE", length = 2500)
	private String exitCode;

	@Column(name = "EXIT_MESSAGE", length = 2500)
	private String exitMessage;

	@Column(name = "LAST_UPDATED")
	private Instant lastUpdated;

	@Column(name = "JOB_CONFIGURATION_LOCATION", length = 2500)
	private String jobConfigurationLocation;

}