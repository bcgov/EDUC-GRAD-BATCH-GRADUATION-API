package ca.bc.gov.educ.api.batchgraduation.model;

public enum JobProperName {
	SGBJ ("Special Graduation Batch Job"),
	STBJ ("Special Achievement Batch Job"),
    YDBJ ("Year-End Distribution Batch Job"),
    MDBJ ("Monthly Distribution Batch Job"),
	URDBJ ("User Req Distribution Batch Job"),
    URPDBJ("PSI Distribution Batch Job"),
    BDBJ ("Blank Distribution Batch Job");

    private final String value;

    JobProperName(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
