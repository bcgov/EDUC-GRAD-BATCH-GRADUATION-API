package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "USER_SCHEDULED_JOBS")
@EqualsAndHashCode(callSuper=false)
public class UserScheduledJobsEntity extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "USER_SCHEDULED_JOBS_ID", nullable = false)
    private UUID id; 

	@Column(name = "JOB_CODE", nullable = false)
    private String jobCode;
	
	@Column(name = "CRON_EXPRESSION")
	private String cronExpression;
	
	@Column(name = "JOB_NAME")
	private String jobName;

	@Column(name = "JOB_PARAMS")
	private String jobParameters;
	
	@Column(name = "STATUS")
    private String status;
	
}