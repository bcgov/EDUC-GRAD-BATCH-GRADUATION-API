package ca.bc.gov.educ.api.batchgraduation.model;

public enum JobSelection {
	SGBJ ("REGALG"),
	STBJ ("TVRRUN"),
	URDBJ ("UserReqDistributionBatchJob"),
    BDBJ ("blankDistributionBatchJob");

    private final String value;

    JobSelection(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
