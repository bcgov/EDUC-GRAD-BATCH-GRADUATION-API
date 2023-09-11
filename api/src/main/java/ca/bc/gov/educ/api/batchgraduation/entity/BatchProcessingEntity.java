package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "BATCH_PROCESSING")
@EqualsAndHashCode(callSuper=false)
public class BatchProcessingEntity extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "BATCH_PROCESSING_ID", nullable = false)
    private UUID id; 
	
	@Column(name = "BATCH_JOB_TYPE_CODE", nullable = false)
    private String jobType;
	
	@Column(name = "CRON_EXPRESSION")
	private String cronExpression;
	
	@Column(name = "SCHEDULE_OCCURRENCE")
	private String scheduleOccurrence;
	
	@Column(name = "ENABLED")
    private String enabled;
	
}