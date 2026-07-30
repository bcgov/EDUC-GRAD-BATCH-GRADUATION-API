package ca.bc.gov.educ.api.batchgraduation.messaging;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchScheduleUpdateEvent {
  private String jobType;
  private String origin;
  private LocalDateTime updatedAt;
}
