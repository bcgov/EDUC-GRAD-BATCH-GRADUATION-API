package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DistributionRequest<T> {
    private String activityCode;
    private List<School> schools;
    private Map<T, DistributionPrintRequest> mapDist;
    private StudentSearchRequest studentSearchRequest;
}
