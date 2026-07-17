package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@Component
public class BatchPipelineRunStatus {

    private Long jobExecutionId;
    private String jobType;
    private String status;
    private String healthStatus;
    private LocalDateTime startTime;
    private LocalDateTime lastHeartbeat;
}
