package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class JobParametersForDistribution {
    private String jobName;
    private StudentSearchRequest payload;
}
