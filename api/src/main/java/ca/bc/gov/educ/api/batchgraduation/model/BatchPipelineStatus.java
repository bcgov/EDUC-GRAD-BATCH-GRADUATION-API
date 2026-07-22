package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class BatchPipelineStatus {

    private boolean running;
    private String message;
    private List<BatchPipelineRunStatus> activeRuns = new ArrayList<>();
    private List<BatchPipelineRunStatus> staleRuns = new ArrayList<>();
}
