package ca.bc.gov.educ.api.batchgraduation.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "BATCH_INFO_DETAILS")
@EqualsAndHashCode(callSuper=false)
public class BatchInfoDetailsEntity  extends BaseEntity {
   
	@Id
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
}