package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class DistributionResponse {
    private String transcriptResponse;
    private String yed2Response;
    private String yedrResponse;
    private String yedbResponse;
    private String mergeProcessResponse;
}
