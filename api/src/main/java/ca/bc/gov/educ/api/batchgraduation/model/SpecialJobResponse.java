package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.Date;

@Data
public class SpecialJobResponse {
    private Long batchId;
    private Date startTime;
    private String status;
    private String jobType;
    private String triggerBy;
    private String jobParameters;

    private String credentialType;
    private String transmissionType;
    private String localDownload;

    //
    private String exception;
}
