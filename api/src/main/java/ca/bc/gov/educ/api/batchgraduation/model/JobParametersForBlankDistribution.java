package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class JobParametersForBlankDistribution {
    private String jobName;
    private String credentialType;
    private BlankCredentialRequest payload;
}
