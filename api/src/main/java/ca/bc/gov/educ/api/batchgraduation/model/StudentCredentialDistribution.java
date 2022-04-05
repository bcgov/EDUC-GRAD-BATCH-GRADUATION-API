package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.UUID;

@Data
public class StudentCredentialDistribution {

	private UUID id;
	private String credentialTypeCode;
	private UUID studentID;
	private String paperType;
	private String schoolOfRecord;
}