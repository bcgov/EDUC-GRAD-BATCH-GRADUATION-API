package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "BATCH_GRAD_ALGORITHM_JOB_HISTORY")
@EqualsAndHashCode(callSuper=false)
public class BatchGradAlgorithmJobHistoryEntity  extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "BATCH_GRAD_ALGORITHM_JOB_HISTORY_ID", nullable = false)
    private UUID id; 
	
	@Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId; 
	
	@Column(name = "START_TIME", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime startTime;
	
	@Column(name = "END_TIME", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime endTime;
	
	@Column(name = "EXPECTED_STUDENTS_PROCESSED", nullable = true)
    private Long expectedStudentsProcessed;
	
	@Column(name = "ACTUAL_STUDENTS_PROCESSED", nullable = true)
    private Long actualStudentsProcessed;
	
	@Column(name = "FAILED_STUDENTS_PROCESSED", nullable = true)
    private Integer failedStudentsProcessed;
	
	@Column(name = "STATUS", nullable = true)
    private String status;
	
	@Column(name = "BATCH_JOB_TRIGGER_CODE",nullable = true)
	private String triggerBy;

	@Column(name = "BATCH_JOB_TYPE_CODE",nullable = true)
	private String jobType;

	@Column(name = "LOCAL_DOWNLOAD",nullable = true)
	private String localDownload;

	@Column(name = "JOB_PARAMS")
	private String jobParameters;
}