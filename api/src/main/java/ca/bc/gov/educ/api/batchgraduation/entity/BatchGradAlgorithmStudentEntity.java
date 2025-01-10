package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "BATCH_GRAD_ALG_STUDENT")
@EqualsAndHashCode(callSuper=false)
public class BatchGradAlgorithmStudentEntity extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "BATCH_GRAD_ALG_STUDENT_ID", nullable = false)
    private UUID id; 
	
	@Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId; 
	
	@Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
	private UUID studentID;

	@Column(name = "GRADUATION_PROGRAM_CODE", nullable = true)
	private String program;

	@Column(name = "SCHOOL_OF_RECORD_ID", nullable = true)
	private UUID schoolOfRecordId;

	@Column(name = "STATUS", nullable = true)
	private String status;
	
	@Column(name = "ERROR", nullable = true)
    private String error;
}