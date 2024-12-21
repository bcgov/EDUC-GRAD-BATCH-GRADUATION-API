package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.UUID;

@Data
public class BlankCredentialDistribution {

	private String credentialTypeCode;
	private String schoolOfRecord;
	private UUID schoolId;
	private int quantity;
	private String paperType;
	private Address address;

}
