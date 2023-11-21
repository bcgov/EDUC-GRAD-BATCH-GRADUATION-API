package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class BlankCredentialDistribution {

	private String credentialTypeCode;
	private String schoolOfRecord;
	private int quantity;
	private String paperType;
	private Address address;
	private String user;

}
