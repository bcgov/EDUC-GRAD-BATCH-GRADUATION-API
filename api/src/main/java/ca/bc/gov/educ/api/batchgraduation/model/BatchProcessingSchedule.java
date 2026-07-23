package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class BatchProcessingSchedule {
    private String jobType;
    private String enabled;
    private String cronExpression;
    private String startTime;
    private String scheduledDateTime;
    private String timeZone;
}
