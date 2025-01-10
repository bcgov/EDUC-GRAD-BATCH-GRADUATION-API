package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DistributionRequest {
    private String activityCode;
    private List<School> schools;
    private Map<UUID, DistributionPrintRequest> mapDist;
    private StudentSearchRequest studentSearchRequest;
}
