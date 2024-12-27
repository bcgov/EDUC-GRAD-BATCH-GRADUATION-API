package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Data
@Entity
@Table(name = "STUDENT_CREDENTIAL_DISTRIBUTION")
@EqualsAndHashCode(callSuper=false)
public class StudentCredentialDistributionEntity extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "STUDENT_CREDENTIAL_DISTRIBUTION_ID", nullable = false)
    private UUID id; 
	
	@Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

	@Column(name = "BATCH_JOB_TYPE_CODE", nullable = true)
	private String jobType;
	
	@Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
	private UUID studentID;

	@Column(name = "SCHOOL_OF_RECORD_ID", nullable = true)
	private String schoolId;

	@Lob
	@Column(name = "PAYLOAD")
	private String payload;

	@Column(name = "STATUS", nullable = true)
	private String status;
	
	@Column(name = "ERROR", nullable = true)
    private String error;
}