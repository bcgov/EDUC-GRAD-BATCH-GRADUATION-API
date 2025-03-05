package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.UUID;

@Data
public class PsiCredentialDistribution {

	private String pen;
	private String psiCode;
	private UUID psiId; // TODO: PSI GUID will be populated from STS
	private String psiYear;
	private UUID studentID;
}
