package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.List;

@Data
public class DistributionResponse {
    private String transcriptResponse;
    private String yed2Response;
    private String yedrResponse;
    private String yedbResponse;
    private String mergeProcessResponse;
    private Long batchId;
    private String localDownload;
    private int totalCyclesCount;
    private int processedCyclesCount;
    private String activityCode;
    private List<School> schools;
}
