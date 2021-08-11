package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.Date;

import lombok.Data;

@Data
public class BaseModel {
	private String createUser;
	private Date createDate;
	private String updateUser;
	private Date updateDate;
}
