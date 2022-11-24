package ca.bc.gov.educ.api.batchgraduation.entity;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
	private Date startTime;
	
	@Column(name = "END_TIME", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date endTime;
	
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