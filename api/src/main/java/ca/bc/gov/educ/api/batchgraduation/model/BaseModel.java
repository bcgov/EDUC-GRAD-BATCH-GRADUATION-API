package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.Date;

@Data
public class BaseModel {
	private String createUser;
	private Date createDate;
	private String updateUser;
	private Date updateDate;
}
