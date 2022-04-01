package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.List;

public class DistributionDataParallelDTO {
    private List<StudentCredentialDistribution> transcriptList;
    private List<StudentCredentialDistribution> certificateList;

    public DistributionDataParallelDTO(List<StudentCredentialDistribution> transcriptList, List<StudentCredentialDistribution> certificateList) {
        this.transcriptList = transcriptList;
        this.certificateList = certificateList;
    }

    public List<StudentCredentialDistribution> transcriptList() {
        return  transcriptList;
    }

    public List<StudentCredentialDistribution> certificateList() {
        return  certificateList;
    }

}
