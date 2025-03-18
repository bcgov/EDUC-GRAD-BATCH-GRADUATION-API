package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.UUID;

@Data
public class PsiCredentialDistribution {

	private String pen;
	private String psiCode;
	private String psiYear;
	private UUID studentID;
}
