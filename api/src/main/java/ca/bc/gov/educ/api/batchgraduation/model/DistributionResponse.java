package ca.bc.gov.educ.api.batchgraduation.model;

import ca.bc.gov.educ.api.batchgraduation.model.institute.District;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class DistributionResponse {
    private String transcriptResponse;
    private String yed2Response;
    private String yedrResponse;
    private String yedbResponse;
    private String mergeProcessResponse;
    private int numberOfPdfs;
    private String jobStatus;
    private Long batchId;
    private String localDownload;
    private String activityCode;
    private List<School> schools = new ArrayList<>();
    private List<District> districts = new ArrayList<>();
    private StudentSearchRequest studentSearchRequest;

    private List<UUID> districtSchools = new ArrayList<>();
}
