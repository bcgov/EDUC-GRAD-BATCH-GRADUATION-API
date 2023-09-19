package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "BATCH_JOB_TYPE_CODE")
public class BatchJobTypeEntity extends BaseEntity {
   
	@Id
	@Column(name = "BATCH_JOB_TYPE_CODE", nullable = false)
    private String code;

	@Column(name = "LABEL", nullable = true)
	private String label;

	@Column(name = "DESCRIPTION", nullable = true)
    private String description;

	@Column(name = "DISPLAY_ORDER", nullable = true)
	private Integer displayOrder;

	@Column(name = "EFFECTIVE_DATE", nullable = true)
	private Date effectiveDate;

	@Column(name = "EXPIRY_DATE", nullable = true)
	private Date expiryDate;
	
}