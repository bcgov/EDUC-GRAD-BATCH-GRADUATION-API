package ca.bc.gov.educ.api.batchgraduation.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "BATCH_GRAD_ALG_ERR_HISTORY")
@EqualsAndHashCode(callSuper=false)
public class BatchGradAlgorithmErrorHistoryEntity extends BaseEntity {
   
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "BATCH_GRAD_ALG_ERR_HISTORY_ID", nullable = false)
    private UUID id; 
	
	@Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId; 
	
	@Column(name = "GRADUATION_STUDENT_RECORD_ID", nullable = false)
	private UUID studentID;
	
	@Column(name = "ERROR", nullable = true)
    private String error;
}