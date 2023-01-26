package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class JobParametersForPsiDistribution {
    private String jobName;
    private String transmissionType;
    private PsiCredentialRequest payload;
}
