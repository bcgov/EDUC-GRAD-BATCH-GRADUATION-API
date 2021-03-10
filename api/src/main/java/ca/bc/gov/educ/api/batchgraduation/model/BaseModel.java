package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.Date;

import lombok.Data;

@Data
public class BaseModel {
	private String createdBy;	
	private Date createdTimestamp;	
	private String updatedBy;	
	private Date updatedTimestamp;
}
