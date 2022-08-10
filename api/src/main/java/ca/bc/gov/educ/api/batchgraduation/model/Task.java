package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class Task {
    private String cronExpression;
    private String jobName;
    private StudentSearchRequest payload;
    private BlankCredentialRequest blankPayLoad;
    private PsiCredentialRequest psiPayLoad;
    private String credentialType;
    private String localDownload;
    private boolean deliveredToUser;
    private String properUserName;
    private String transmissionType;
}
