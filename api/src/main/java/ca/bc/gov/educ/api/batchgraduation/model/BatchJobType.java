package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class BatchJobType extends BaseModel {

	private String code;
	private String label;
	private String description;
	private Integer displayOrder;
	private Date effectiveDate;
	private Date expiryDate;
	private String paperType;
	
	@Override
	public String toString() {
		return "BatchJobType [code=" + code + ", label=" + label + ", description=" + description + ", displayOrder="
				+ displayOrder + ", effectiveDate=" + effectiveDate + ", expiryDate=" + expiryDate + "]";
	}
	
	
}
