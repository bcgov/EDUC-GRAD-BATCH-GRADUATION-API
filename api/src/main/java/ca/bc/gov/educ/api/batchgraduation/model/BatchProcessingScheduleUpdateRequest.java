package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class BatchProcessingScheduleUpdateRequest {
    private String scheduledDateTime;
    private String timeZone;
}
