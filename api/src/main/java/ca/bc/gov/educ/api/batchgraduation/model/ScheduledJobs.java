package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class ScheduledJobs {
    private String jobId;
    private String jobName;
    private String scheduledBy;
    private String status;
}
