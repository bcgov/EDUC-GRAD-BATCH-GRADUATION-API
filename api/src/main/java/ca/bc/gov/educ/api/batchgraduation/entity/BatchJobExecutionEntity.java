package ca.bc.gov.educ.api.batchgraduation.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "BATCH_JOB_EXECUTION")
@EqualsAndHashCode(callSuper=false)
public class BatchJobExecutionEntity {

	@Id
	@Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

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
}