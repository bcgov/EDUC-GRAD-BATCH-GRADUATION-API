package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchJobResponse {
    private Long batchId;
    private LocalDateTime startTime;
    private String status;
    private String jobType;
    private String triggerBy;
    private String jobParameters;

    private String credentialType;
    private String transmissionType;
    private String localDownload;

    private String exception;
    private String message;
}
