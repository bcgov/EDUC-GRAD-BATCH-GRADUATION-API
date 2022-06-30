package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class ScheduledJobs {
    private int jobId;
    private String jobName;
    private String scheduledBy;
    private String status;
}
